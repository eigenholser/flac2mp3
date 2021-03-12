package com.eigenholser.flac2mp3

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import kotlin.io.path.ExperimentalPathApi



@ExperimentalPathApi
fun main(args: Array<String>) {
    val db = DbSettings.db
    FlacDatabase.createDatabase()

    File(Config.flacRoot)
        .walk()
        .filter {
            it.extension == "flac"
        }
        .forEach { file ->
            val flacfile = file.absolutePath
            val fsize = Files.getAttribute(file.toPath(), "size") as Long
            val mtime = Files.getAttribute(file.toPath(), "lastModifiedTime") as FileTime
            val tags = Tag.readFlacTags(flacfile)
            println(tags)
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

    FlacDatabase.getAllFlacRows().forEach {
        val flacFileAbsolute = File("${Config.flacRoot}/${it[Flac.flacfile]}")
        val flacAlbumPathAbsolute = File(flacFileAbsolute.toString().removeSuffix("/${flacFileAbsolute.name}"))
        val flacAlbumArtFile = File("$flacAlbumPathAbsolute/${Config.albumArtFile}")
        val flacFileTrackName = flacFileAbsolute.name
        val currentAlbum = flacAlbumPathAbsolute.toString()
            .removePrefix("${Config.flacRoot}/")
            .removeSuffix("/${flacFileTrackName}")

        val mp3AlbumPathAbsolute = File("${Config.mp3Root}/$currentAlbum").toPath()
        val mp3FileAbsolute = File("$mp3AlbumPathAbsolute/${flacFileAbsolute.nameWithoutExtension}.mp3")

        if (!switchAlbum && currentAlbum != nextAlbum) {
            switchAlbum = true
            nextAlbum = currentAlbum
        }

        val trackIsCurrent = isTrackCurrent(flacFileAbsolute, it[Flac.fsize], it[Flac.mtime])

        if (switchAlbum) {
            switchAlbum = false
            Files.createDirectories(mp3AlbumPathAbsolute)
            ImageScaler.scaleImage(flacAlbumPathAbsolute.toString(), mp3AlbumPathAbsolute.toString())
        }

        if (!trackIsCurrent || !mp3FileAbsolute.exists()) {
            LameFlac2Mp3.flac2mp3(flacFileAbsolute.toString(), mp3FileAbsolute.toString(), mp3AlbumPathAbsolute)
            val flacTags = Tag.readFlacTags(flacFileAbsolute.toString())
            Tag.writeMp3Tags(mp3FileAbsolute.toString(), mp3AlbumPathAbsolute.toString(), flacTags)
        }
    }
}

fun isTrackCurrent(flacFileAbsolutePath: File, storedFsize: Long, storedMtime: Long): Boolean {
    val fsize = Files.getAttribute(flacFileAbsolutePath.toPath(), "size")
    val mtime = Files.getAttribute(flacFileAbsolutePath.toPath(), "lastModifiedTime") as FileTime

    if (storedFsize == fsize && storedMtime == mtime.toMillis()) {
        return true
    }

    return false
}

