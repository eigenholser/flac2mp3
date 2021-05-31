package com.eigenholser.flac2mp3

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.ResultRow
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.ExperimentalPathApi


@ExperimentalPathApi
fun main(args: Array<String>) {
    val db = DbSettings.db
    FlacDatabase.createDatabase()

    var switchAlbum = false
    var nextAlbum = ""
    var prevMp3AlbumPath: Path? = null

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
            if (!switchAlbum && it.currentAlbum != nextAlbum) {
                switchAlbum = true
                nextAlbum = it.currentAlbum
                deleteMp3CoverArt(prevMp3AlbumPath)
                prevMp3AlbumPath = it.mp3AlbumPathAbsolute
            }
            if (switchAlbum) {
                switchAlbum = false
                Files.createDirectories(it.mp3AlbumPathAbsolute)
                // TODO: Does MP3 exist && FLAC album_art.png exist?
                // TODO: If above is true, is FLAC album_art.png newer than MP3 file?
                // TODO: If this is not null... we have album art. Then read mtime on MP3 file.
                if (mp3FileExists(it)) {
                    val flag = Tag.albumArtExists(it.mp3FileAbsolute)
                    if (!flag) {
                        println("******************** ALBUM ART EXISTS ********************")
                    }
                }

                ImageScaler.scaleImage(
                    it.flacAlbumPathAbsolute.toString(),
                    it.mp3AlbumPathAbsolute.toString()
                )
            }
            // TODO: Only do the stuff below if we're building new MP3
            if (!isTrackCurrent(it)) {
                LameFlac2Mp3.flac2mp3(
                    it.flacFileAbsolute.toString(),
                    it.mp3FileAbsolute.toString(),
                    it.mp3AlbumPathAbsolute
                )

                val flacTags = Tag.readFlacTags(it.flacFileAbsolute.toString())
                Tag.writeMp3Tags(it.mp3FileAbsolute.toString(), it.mp3AlbumPathAbsolute.toString(), flacTags)
            }

            // TODO: Only do the stuff below if we're updating album art
            // TODO: Write the code to update album art.
        }
    // Get the last album
    deleteMp3CoverArt(prevMp3AlbumPath)
}

data class TrackData(
    val flacFileAbsolute: File, val flacAlbumPathAbsolute: File,
    val flacFileTrackName: String, val currentAlbum: String, val mp3AlbumPathAbsolute: Path,
    val mp3FileAbsolute: File, val fsize: Long, val mtime: Long
)

fun convertRow(row: ResultRow): TrackData {
    return convertRow(row[Flac.flacfile], row[Flac.fsize], row[Flac.mtime])
}

fun convertRow(flacfile: String, fsize: Long, mtime: Long): TrackData {
    val flacFileAbsolute = File("${flacfile}")
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

fun isAlbumArtCurrent(trackData: TrackData): Boolean {
    // TODO: check state
    return true
}

fun shouldWeProcessThisTrack(trackData: TrackData): Boolean {
    return (!isTrackCurrent(trackData) || shouldWeUpdateAlbumArt(trackData))
}

fun shouldWeUpdateAlbumArt(trackData: TrackData): Boolean {
    // do something
    return true
}

fun deleteMp3CoverArt(mp3AlbumPathAbsolute: Path?): Boolean {
    if (mp3AlbumPathAbsolute != null) {
        val mp3CoverArtFile = File("${mp3AlbumPathAbsolute.toString()}/${Config.coverArtFile}")
        return mp3CoverArtFile.delete()
    }
    return false
}