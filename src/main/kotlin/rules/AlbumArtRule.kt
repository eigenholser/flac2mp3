package com.eigenholser.flac2mp3.rules

import com.eigenholser.flac2mp3.DestType
import com.eigenholser.flac2mp3.ImageScaler
import com.eigenholser.flac2mp3.Tag
import com.eigenholser.flac2mp3.TrackData
import com.eigenholser.flac2mp3.states.AlbumStates
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.states.api.FiniteStateMachine

interface AlbumArtRule: Rule {
    val rulePriority: Int

    override fun execute(facts: Facts) {
        val trackData = facts.get<TrackData>(AlbumArtFacts.TRACK_DATA.toString())
        when (AlbumStates.valueOf(facts.get<FiniteStateMachine>(AlbumArtFacts.ALBUM_STATE.toString()).currentState.name)) {
            AlbumStates.NEW_ALBUM -> {
                ImageScaler.scaleImage(
                    trackData.flacAlbumPathAbsolute.toString(),
                    trackData.mp3AlbumPathAbsolute.toString(),
                    DestType.COVER
                )
                Tag.updateAlbumArtField(
                    trackData.mp3FileAbsolute.toString(),
                    trackData.mp3AlbumPathAbsolute.toString()
                )
            }
            AlbumStates.EXISTING_ALBUM -> {
                Tag.updateAlbumArtField(
                    trackData.mp3FileAbsolute.toString(),
                    trackData.mp3AlbumPathAbsolute.toString()
                )
            }
        }
    }

    override fun getPriority(): Int {
        return rulePriority
    }

    override fun compareTo(other: Rule): Int {
        return when {
            this.priority > other.priority -> 1
            this.priority < other.priority -> -1
            else -> this.name.compareTo(other.name)
        }
    }
}