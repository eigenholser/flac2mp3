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
    val configFile = System.getenv("HOME") + "/flac2mp3.properties"
    println("Reading from $configFile")

    val config = ConfigFactory.parseFile(File(configFile))
    val mp3Root = config.extract<String>("mp3_root")
    println(mp3Root)
    val flacRoot = config.extract<String>("flac_root")
    println(flacRoot)
    val albumArtFile = config.extract<String>("album_art.name")
    println(albumArtFile)
    val coverResolution = config.extract<Int>("album_art.resolution.cover")
    println(coverResolution)
    val thumbnailResolution = config.extract<Int>("album_art.resolution.thumb")
    println(thumbnailResolution)

    val db = DbSettings.db
    FlacDatabase.createDatabase()

    File(flacRoot)
        .walk()
        .filter {
            it.extension == "flac"
        }
        .forEach { file ->
            println("flacfile: $file")
            print("fsize: ${Files.getAttribute(file.toPath(), "size")}  ")
            println("mtime: ${Files.getAttribute(file.toPath(), "lastModifiedTime")}")
            val flacfile = file.absolutePath
            val fsize = Files.getAttribute(file.toPath(), "size") as Long
            val mtime = Files.getAttribute(file.toPath(), "lastModifiedTime") as FileTime
            val tags = Tag.readFlacTags(flacfile)
            println(tags)
            val flacRelativePath = flacfile.removePrefix("$flacRoot/")

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
        val flacFileAbsolute = File("$flacRoot/${it[Flac.flacfile]}")
        val flacAlbumPathAbsolute = File(flacFileAbsolute.toString().removeSuffix("/${flacFileAbsolute.name}"))
        val flacAlbumArtFile = File("$flacAlbumPathAbsolute/$albumArtFile")
        val flacFileTrackName = flacFileAbsolute.name
        val currentAlbum = flacAlbumPathAbsolute.toString()
            .removePrefix("$flacRoot/")
            .removeSuffix("/${flacFileTrackName}")

        val mp3AlbumPathAbsolute = File("$mp3Root/$currentAlbum").toPath()
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

