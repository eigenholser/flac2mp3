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
        val flacDb = flacRoot.plus("/").plus(config.extract<String>("flacdb.filename"))
        val albumArtFile = config.extract<String>("album_art.name.full")
        val thumbArtFile = config.extract<String>("album_art.name.thumb")
        val coverArtFile = config.extract<String>("album_art.name.cover")
        val coverResolution = config.extract<Int>("album_art.resolution.cover")
        val thumbnailResolution = config.extract<Int>("album_art.resolution.thumb")
        val bitRate = config.extract<Int>("mp3.bitrate")
        val quality = config.extract<Int>("mp3.quality")
        val lamePath = config.extract<String>("lame.path")
    }
}