package com.eigenholser.flac2mp3.rules

import com.eigenholser.flac2mp3.*
import org.jeasy.rules.api.Facts
import kotlin.io.path.ExperimentalPathApi

class ArtUpdateIDv3: AlbumArtRule {
    override val rulePriority = 2

    override fun getName(): String {
        return AlbumArtRules.MP3_TAGGED_ART_UPDATED.toString()
    }

    override fun getDescription(): String {
        return "Existing MP3 file has album art tag and album art PNG updated in FLAC album."
    }

    @ExperimentalPathApi
    override fun evaluate(facts: Facts): Boolean {
        val trackData = facts.get<TrackData>(AlbumArtFacts.TRACK_DATA.toString())
        return isAlbumArtUpdated(trackData)
    }
}