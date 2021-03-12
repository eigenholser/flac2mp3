package com.eigenholser.flac2mp3

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import java.io.File

class Config {
    companion object {
        val configFile = System.getenv("HOME") + "/flac2mp3.conf"
        val config = ConfigFactory.parseFile(File(configFile))
        val mp3Root = config.extract<String>("mp3_root")
        val flacRoot = config.extract<String>("flac_root")
        val albumArtFile = config.extract<String>("album_art.name")
        val coverResolution = config.extract<Int>("album_art.resolution.cover")
        val thumbnailResolution = config.extract<Int>("album_art.resolution.thumb")
    }
}