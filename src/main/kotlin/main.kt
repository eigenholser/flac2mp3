package com.eigenholser.flac2mp3

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
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

    File(flacRoot).walk().filter {
        it.extension == "flac"
    }.forEach { file ->
        println("flacfile: $file")
        print("fsize: ${Files.getAttribute(file.toPath(), "size")}  ")
        println("mtime: ${Files.getAttribute(file.toPath(), "lastModifiedTime")}")
        val flacfile = file.absolutePath
        val fsize = Files.getAttribute(file.toPath(), "size") as Long
        val mtime = Files.getAttribute(file.toPath(), "lastModifiedTime") as FileTime
        val tags = FlacTag.readFlacTags(flacfile)
        println(tags)
        FlacDatabase.insertFlac(flacfile, tags.cddb, tags.track, fsize, mtime.toMillis())
    }
}

