package com.eigenholser.flac2mp3.rules

import com.eigenholser.flac2mp3.*
import org.jeasy.rules.api.Facts
import kotlin.io.path.ExperimentalPathApi

class ArtNewMp3: AlbumArtRule {
    override val rulePriority = 1

    override fun getName(): String {
        return AlbumArtRules.NEW_MP3_ART_EXISTS.toString()
    }

    override fun getDescription(): String {
        return "New MP3 file and album art PNG exists in FLAC album."
    }

    @OptIn(ExperimentalPathApi::class)
    override fun evaluate(facts: Facts): Boolean {
        val trackData = facts.get<TrackData>(AlbumArtFacts.TRACK_DATA.toString())
        return !mp3FileExists(trackData) && albumArtPNGExists(trackData)
    }
}