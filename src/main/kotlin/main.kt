package com.eigenholser.flac2mp3

import com.eigenholser.flac2mp3.rules.NewAlbum
import com.eigenholser.flac2mp3.states.SwitchAlbum
import org.jeasy.rules.api.Fact
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.jeasy.states.api.AbstractEvent
import org.jeasy.states.api.State
import org.jeasy.states.core.FiniteStateMachineBuilder
import org.jeasy.states.core.TransitionBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists

data class ConversionState(/* var switchAlbum: Boolean = false, */var nextAlbum: String = "",
                           var prevMp3AlbumPath: Path? = null/*, var albumArtUpdate: Boolean = false*/)

class NewAlbumEvent : AbstractEvent()
class ExistingAlbumEvent : AbstractEvent()

enum class AlbumState {
    NEW_ALBUM,
    EXISTING_ALBUM
}

enum class AlbumRule {
    NEW_ALBUM
}

enum class AlbumFact {
    ALBUM_STATE, CURRENT_ALBUM, NEXT_ALBUM
}

@ExperimentalPathApi
fun main(args: Array<String>) {
    val db = DbSettings.db
    FlacDatabase.createDatabase()

    val state = ConversionState()
    val newAlbum = State(AlbumState.NEW_ALBUM.toString())
    val existingAlbum = State(AlbumState.EXISTING_ALBUM.toString())
    val states = mutableSetOf(newAlbum, existingAlbum)

    val newAlbumTx = TransitionBuilder()
        .name(AlbumState.NEW_ALBUM.toString())
        .sourceState(existingAlbum)
        .eventType(NewAlbumEvent::class.java)
        .eventHandler(SwitchAlbum())
        .targetState(newAlbum)
        .build()

    val existingAlbumTx = TransitionBuilder()
        .name(AlbumState.EXISTING_ALBUM.toString())
        .sourceState(newAlbum)
        .eventType(ExistingAlbumEvent::class.java)
        .eventHandler(SwitchAlbum())
        .targetState(existingAlbum)
        .build()

    val albumStateMachine = FiniteStateMachineBuilder(states, existingAlbum)
        .registerTransition(newAlbumTx)
        .registerTransition(existingAlbumTx)
        .build()

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
        .filter (::shouldWeProcessThisTrack)
        .forEach {
            println(it)

            val rules = Rules(NewAlbum(albumStateMachine))
            val facts = Facts()
            facts.add(Fact(AlbumFact.ALBUM_STATE.toString(), albumStateMachine))
            facts.add(Fact(AlbumFact.CURRENT_ALBUM.toString(), it.currentAlbum))
            facts.add(Fact(AlbumFact.NEXT_ALBUM.toString(), state.nextAlbum))
            rulesEngine.fire(rules, facts)

            if (AlbumState.valueOf(albumStateMachine.currentState.name) == AlbumState.NEW_ALBUM) {
                albumStateMachine.fire(ExistingAlbumEvent())
                state.nextAlbum = it.currentAlbum
                deleteMp3CoverArt(state.prevMp3AlbumPath)
                state.prevMp3AlbumPath = it.mp3AlbumPathAbsolute
                Files.createDirectories(it.mp3AlbumPathAbsolute)

                if (shouldWeUpdateAlbumArt(it)) {
                    ImageScaler.scaleImage(
                        it.flacAlbumPathAbsolute.toString(),
                        it.mp3AlbumPathAbsolute.toString()
                    )
                }
            }

            if (AlbumState.valueOf(albumStateMachine.currentState.name) == AlbumState.EXISTING_ALBUM) {
                if (mp3FileExists(it)) {
                    if (shouldWeUpdateAlbumArt(it)) {
                        Tag.updateAlbumArtField(
                            it.mp3FileAbsolute.toString(),
                            it.mp3AlbumPathAbsolute.toString()
                        )
                    }
                }

                if (!isTrackCurrent(it)) {
                    LameFlac2Mp3.flac2mp3(
                        it.flacFileAbsolute.toString(),
                        it.mp3FileAbsolute.toString(),
                        it.mp3AlbumPathAbsolute
                    )

                    val flacTags = Tag.readFlacTags(it.flacFileAbsolute.toString())
                    Tag.writeMp3Tags(it.mp3FileAbsolute.toString(), it.mp3AlbumPathAbsolute.toString(), flacTags)
                }
            }
        }
    // Delete the last album art
    deleteMp3CoverArt(state.prevMp3AlbumPath)
}

data class TrackData(
    val flacFileAbsolute: File, val flacAlbumPathAbsolute: File,
    val flacFileTrackName: String, val currentAlbum: String, val mp3AlbumPathAbsolute: Path,
    val mp3FileAbsolute: File, val fsize: Long, val mtime: Long
)

fun convertRow(flacfile: String, fsize: Long, mtime: Long): TrackData {
    val flacFileAbsolute = File(flacfile)
    val flacAlbumPathAbsolute = File(flacFileAbsolute.toString().removeSuffix("/${flacFileAbsolute.name}"))
    val flacFileTrackName = flacFileAbsolute.name
    val currentAlbum = flacAlbumPathAbsolute.toString()
        .removePrefix("${Config.flacRoot}/")
        .removeSuffix("/${flacFileTrackName}")
    val mp3AlbumPathAbsolute = File("${Config.mp3Root}/$currentAlbum").toPath()
    val mp3FileAbsolute = File("$mp3AlbumPathAbsolute/${flacFileAbsolute.nameWithoutExtension}.mp3")

    return TrackData(
        flacFileAbsolute, flacAlbumPathAbsolute, flacFileTrackName,
        currentAlbum, mp3AlbumPathAbsolute, mp3FileAbsolute, fsize, mtime
    )
}

fun isTrackCurrent(trackData: TrackData): Boolean {
    if (mp3FileExists(trackData)) {
        val flacMtime = Files.getAttribute(trackData.flacFileAbsolute.toPath(), "lastModifiedTime") as FileTime
        val mp3Mtime = Files.getAttribute(trackData.mp3FileAbsolute.toPath(), "lastModifiedTime") as FileTime
        return (flacMtime.toMillis() < mp3Mtime.toMillis())
    }
    return false
}

fun mp3FileExists(trackData: TrackData): Boolean {
    return trackData.mp3FileAbsolute.exists()
}
@ExperimentalPathApi
fun albumArtPNGExists(trackData: TrackData): Boolean = trackData.flacAlbumPathAbsolute.toPath().resolve(Config.albumArtFile).exists()

fun isAlbumArtCurrent(trackData: TrackData): Boolean {
    if (Tag.albumArtTagExists(trackData.mp3FileAbsolute)) {
        val albumArtMtime = Files.getAttribute(trackData.flacAlbumPathAbsolute.toPath().resolve(Config.albumArtFile), "lastModifiedTime") as FileTime
        val mp3Mtime = Files.getAttribute(trackData.mp3FileAbsolute.toPath(), "lastModifiedTime") as FileTime
        return (albumArtMtime.toMillis() > mp3Mtime.toMillis())
    }
    return true
}

@ExperimentalPathApi
fun shouldWeProcessThisTrack(trackData: TrackData): Boolean {
    return (!isTrackCurrent(trackData) || shouldWeUpdateAlbumArt(trackData))
}

//1. MP3 file not found: Scale art
//2. MP3 file found: No art tagged, art found in flac
//3. MP3 file found: Art tagged, art found in flac, found art is newer than MP3 file
@ExperimentalPathApi
fun shouldWeUpdateAlbumArt(trackData: TrackData): Boolean =
    if (mp3FileExists(trackData) && albumArtPNGExists(trackData) && Tag.albumArtTagExists(trackData.mp3FileAbsolute)) {
        ImageScaler.logger.warning("Determination: " + isAlbumArtCurrent(trackData))
        isAlbumArtCurrent(trackData)
    } else if (mp3FileExists(trackData) && albumArtPNGExists(trackData) && !Tag.albumArtTagExists(trackData.mp3FileAbsolute)) {
        true
    } else if (!mp3FileExists(trackData) && albumArtPNGExists(trackData)) {
        true
    } else if (!albumArtPNGExists(trackData)) {
        ImageScaler.logger.warning(trackData.flacAlbumPathAbsolute.toPath().toString())
        false
    } else true




fun deleteMp3CoverArt(mp3AlbumPathAbsolute: Path?): Boolean {
    if (mp3AlbumPathAbsolute != null) {
        val mp3CoverArtFile = File("${mp3AlbumPathAbsolute.toString()}/${Config.coverArtFile}")
        return mp3CoverArtFile.delete()
    }
    return false
}