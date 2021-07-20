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

    File(Config.flacRoot)
        .walk()
        .filter {it.extension == "flac"}
        .forEach { file ->
            val flacfile = file.absolutePath
            val fsize = Files.getAttribute(file.toPath(), "size") as Long
            val mtime = Files.getAttribute(file.toPath(), "lastModifiedTime") as FileTime
            val tags = Tag.readFlacTags(flacfile)
            val flacRelativePath = flacfile.removePrefix("${Config.flacRoot}/")

            //val flacRow = FlacDatabase.getByCddbAndTrack(tags.cddb, tags.track)

            try {
                FlacDatabase.insertFlac(flacRelativePath, tags.cddb, tags.track, fsize, mtime.toMillis())
            } catch (e: ExposedSQLException) {
                println("EXISTS: $tags")
            }
        }

    var switchAlbum = false
    var nextAlbum = ""

    FlacDatabase.getAllFlacRows()
        .map (::convertRow)
        .filter (::processTrack)
        .forEach {
            println(it)

            if (!switchAlbum && it.currentAlbum != nextAlbum) {
                switchAlbum = true
                nextAlbum = it.currentAlbum
            }
            if (switchAlbum) {
                switchAlbum = false
                Files.createDirectories(it.mp3AlbumPathAbsolute)
                ImageScaler.scaleImage(
                    it.flacAlbumPathAbsolute.toString(),
                    it.mp3AlbumPathAbsolute.toString()
                )
            }
            LameFlac2Mp3.flac2mp3(
                it.flacFileAbsolute.toString(),
                it.mp3FileAbsolute.toString(),
                it.mp3AlbumPathAbsolute
            )
            val flacTags = Tag.readFlacTags(it.flacFileAbsolute.toString())
            Tag.writeMp3Tags(it.mp3FileAbsolute.toString(), it.mp3AlbumPathAbsolute.toString(), flacTags)
        }
}

data class TrackData(
    val flacFileAbsolute: File, val flacAlbumPathAbsolute: File,
    val flacFileTrackName: String, val currentAlbum: String, val mp3AlbumPathAbsolute: Path,
    val mp3FileAbsolute: File, val fsize: Long, val mtime: Long
)

fun convertRow(row: ResultRow): TrackData {
    val flacFileAbsolute = File("${Config.flacRoot}/${row[Flac.flacfile]}")
    val flacAlbumPathAbsolute = File(flacFileAbsolute.toString().removeSuffix("/${flacFileAbsolute.name}"))
    val flacFileTrackName = flacFileAbsolute.name
    val currentAlbum = flacAlbumPathAbsolute.toString()
        .removePrefix("${Config.flacRoot}/")
        .removeSuffix("/${flacFileTrackName}")
    val mp3AlbumPathAbsolute = File("${Config.mp3Root}/$currentAlbum").toPath()
    val mp3FileAbsolute = File("$mp3AlbumPathAbsolute/${flacFileAbsolute.nameWithoutExtension}.mp3")

    return TrackData(
        flacFileAbsolute, flacAlbumPathAbsolute, flacFileTrackName,
        currentAlbum, mp3AlbumPathAbsolute, mp3FileAbsolute, row[Flac.fsize], row[Flac.mtime]
    )
}

fun isTrackCurrent(trackData: TrackData): Boolean {
    val fsize = Files.getAttribute(trackData.flacFileAbsolute.toPath(), "size")
    val mtime = Files.getAttribute(trackData.flacFileAbsolute.toPath(), "lastModifiedTime") as FileTime
    return (trackData.fsize == fsize && trackData.mtime == mtime.toMillis())
}

fun mp3FileExists(trackData: TrackData): Boolean {
    return trackData.mp3FileAbsolute.exists()
}

fun processTrack(trackData: TrackData): Boolean {
    return (!isTrackCurrent(trackData) || !mp3FileExists(trackData))
}