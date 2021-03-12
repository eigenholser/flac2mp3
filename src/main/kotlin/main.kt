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

    //ImageScaler.scaleImage("src/main/resources/image.jpg", "src/main/resources/", DestType.THUMB)
    //ImageScaler.scaleImage("src/main/resources/image.jpg", "src/main/resources/")

    /*
    val process = ProcessBuilder(
        //"/bin/ls", "-l", "/")
        "/usr/bin/play",
        System.getenv("HOME") + "/Music/mp3/Deep_Purple/Live_Long_Beach_Arena_2-27-76_Disc_01/01.Intro.mp3"
    )
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)

        .start()
        .waitFor()
    */

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
            val tags = FlacTag.readFlacTags(flacfile)
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

        if (!switchAlbum && currentAlbum != nextAlbum) {
            switchAlbum = true
            nextAlbum = currentAlbum
        }

        val trackIsCurrent = isTrackCurrent(flacFileAbsolute, it[Flac.fsize], it[Flac.mtime])

        if (switchAlbum) {
            switchAlbum = false
            println()
            println("*******************************************")
            println(flacAlbumPathAbsolute)
            print("$flacAlbumArtFile ")
            println(if (flacAlbumArtFile.exists()) "EXISTS" else "NOTEXISTS")
            println("Current Album: $currentAlbum")
        }

        println(flacFileAbsolute)
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

