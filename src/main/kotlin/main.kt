package com.eigenholser.flac2mp3

import com.eigenholser.flac2mp3.rules.*
import com.eigenholser.flac2mp3.states.AlbumState.albumStateMachine
import com.eigenholser.flac2mp3.states.AlbumState.state
import com.eigenholser.flac2mp3.states.AlbumStates
import com.eigenholser.flac2mp3.states.ExistingAlbumEvent
import org.jeasy.rules.api.Fact
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules
import org.jeasy.rules.api.RulesEngineParameters
import org.jeasy.rules.core.DefaultRulesEngine
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.util.logging.Logger
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists

val logger: Logger = Logger.getLogger("main")

@ExperimentalPathApi
fun main(args: Array<String>) {
    /* Leaving the DB stuff for now. May return to it later. */
    // val db = DbSettings.db
    // FlacDatabase.createDatabase()

    val rulesEngine = DefaultRulesEngine()

    File(Config.flacRoot)
        .walk()
        .filter {it.extension == "flac"}
        .map { file ->
            val flacfile = file.absolutePath
            val fsize = Files.getAttribute(file.toPath(), "size") as Long
            val mtime = Files.getAttribute(file.toPath(), "lastModifiedTime") as FileTime
            convertRow(flacfile, fsize, mtime.toMillis())
        }
        .filter (::isThisTrackStale)
        .forEach {
            logger.info(it.toString())

            val rules = Rules(NewAlbum(albumStateMachine))
            val facts = Facts()
            facts.add(Fact(AlbumFact.ALBUM_STATE.toString(), albumStateMachine))
            facts.add(Fact(AlbumFact.CURRENT_ALBUM.toString(), it.currentAlbum))
            facts.add(Fact(AlbumFact.NEXT_ALBUM.toString(), state.nextAlbum))
            rulesEngine.fire(rules, facts)

            val albumArtFacts = Facts()
            albumArtFacts.add(Fact(AlbumArtFacts.TRACK_DATA.toString(), it))
            albumArtFacts.add(Fact(AlbumArtFacts.ALBUM_STATE.toString(), albumStateMachine))
            val parameters = RulesEngineParameters()
                .skipOnFirstAppliedRule(true)

            when (AlbumStates.valueOf(albumStateMachine.currentState.name)) {
                AlbumStates.NEW_ALBUM -> {
                    deleteMp3CoverArt(state.prevMp3AlbumPath)

                    state.nextAlbum = it.currentAlbum
                    state.prevMp3AlbumPath = it.mp3Album
                    Files.createDirectories(Paths.get(it.mp3Album))

                    val albumArtRulesEngine = DefaultRulesEngine(parameters)
                    val albumArtRules = Rules(ArtNewMp3(), ArtUpdateIDv3())
                    albumArtRulesEngine.fire(albumArtRules, albumArtFacts)

                    albumStateMachine.fire(ExistingAlbumEvent())
                }
                AlbumStates.EXISTING_ALBUM -> {
                    val albumArtRulesEngine = DefaultRulesEngine(parameters)
                    val albumArtRules = Rules(ArtNewMp3(), ArtUpdateIDv3())
                    albumArtRulesEngine.fire(albumArtRules, albumArtFacts)
                }
            }
            if (!isTrackCurrent(it)) {
                LameFlac2Mp3.flac2mp3(
                    it.flacFile,
                    it.mp3File
                )

                val flacTags = Tag.readFlacTags(it.flacFile)
                Tag.writeMp3Tags(it.mp3File, it.mp3Album, flacTags)
            }
        }
    // Delete the last album art
    deleteMp3CoverArt(state.prevMp3AlbumPath)
}

data class TrackData(
    val flacFile: String,
    val flacAlbum: String,
    val currentAlbum: String,
    val mp3Album: String,
    val mp3File: String,
    val fsize: Long,
    val mtime: Long
)

fun convertRow(flacFile: String, fsize: Long, mtime: Long): TrackData {
    val MP3_EXTENSION = "mp3"
    val EXTENSION_SEPARATOR = "."
    val DIR_SEPARATOR = "/"

    val flacTrackname = File(flacFile).name
    val trackName = File(flacFile).nameWithoutExtension
    val mp3TrackName = trackName + EXTENSION_SEPARATOR + MP3_EXTENSION
    val flacAlbum = flacFile.removeSuffix(DIR_SEPARATOR + flacTrackname)
    val currentAlbum = flacAlbum
        .removePrefix(Config.flacRoot + DIR_SEPARATOR)
        .removeSuffix(DIR_SEPARATOR + flacTrackname)
    val mp3Album = Config.mp3Root + DIR_SEPARATOR + currentAlbum
    val mp3File = mp3Album + DIR_SEPARATOR + mp3TrackName

    return TrackData(
        flacFile = flacFile,
        flacAlbum = flacAlbum,
        currentAlbum = currentAlbum,
        mp3Album = mp3Album,
        mp3File = mp3File,
        fsize = fsize,
        mtime = mtime
    )
}

fun isTrackCurrent(trackData: TrackData): Boolean {
    if (mp3FileExists(trackData)) {
        val flacMtime = Files.getAttribute(Paths.get(trackData.flacFile), "lastModifiedTime") as FileTime
        val mp3Mtime = Files.getAttribute(Paths.get(trackData.mp3File), "lastModifiedTime") as FileTime
        return (flacMtime.toMillis() < mp3Mtime.toMillis())
    }
    return false
}

fun mp3FileExists(trackData: TrackData): Boolean = File(trackData.mp3File).exists()

@ExperimentalPathApi
fun albumArtPNGExists(trackData: TrackData): Boolean =
    Paths.get(trackData.flacAlbum).resolve(Config.albumArtFile).exists()

@ExperimentalPathApi
fun isAlbumArtUpdated(trackData: TrackData): Boolean =
    if (mp3FileExists(trackData) && albumArtPNGExists(trackData)) {
        val albumArtMtime = Files.getAttribute(
            Paths.get(trackData.flacAlbum).resolve(Config.albumArtFile), "lastModifiedTime"
        ) as FileTime
        val mp3Mtime = Files.getAttribute(Paths.get(trackData.mp3File), "lastModifiedTime") as FileTime
        (Tag.albumArtTagExists(trackData.mp3File) && albumArtMtime.toMillis() > mp3Mtime.toMillis())
                || !Tag.albumArtTagExists(trackData.mp3File)
    } else false

@ExperimentalPathApi
fun isThisTrackStale(trackData: TrackData): Boolean {
    return (!isTrackCurrent(trackData) || isAlbumArtUpdated(trackData))
}

fun deleteMp3CoverArt(mp3Album: String): Boolean {
    val mp3CoverArtFile = File("$mp3Album/${Config.coverArtFile}")
    if (mp3CoverArtFile.exists())
        return mp3CoverArtFile.delete()
    return false
}
