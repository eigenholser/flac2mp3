package com.eigenholser.flac2mp3.rules

import com.eigenholser.flac2mp3.AlbumFact
import com.eigenholser.flac2mp3.AlbumRule
import com.eigenholser.flac2mp3.AlbumState
import org.jeasy.rules.api.Facts
import org.jeasy.states.api.FiniteStateMachine

class NewAlbum: ConversionRule {
    override fun getName(): String {
        return AlbumRule.NEW_ALBUM.toString()
    }

    override fun evaluate(facts: Facts): Boolean {
        val albumState = facts.get<FiniteStateMachine>(AlbumFact.ALBUM_STATE.toString())
        val currentAlbum = facts.get<String>(AlbumFact.CURRENT_ALBUM.toString())
        val nextAlbum = facts.get<String>(AlbumFact.NEXT_ALBUM.toString())

        return AlbumState.valueOf(albumState.currentState.name) == AlbumState.EXISTING_ALBUM && currentAlbum != nextAlbum
    }
}