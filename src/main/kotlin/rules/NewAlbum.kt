package com.eigenholser.flac2mp3.rules

import com.eigenholser.flac2mp3.states.AlbumStates
import com.eigenholser.flac2mp3.states.NewAlbumEvent
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.states.api.FiniteStateMachine

class NewAlbum(private val albumStateMachine: FiniteStateMachine): Rule {
    override fun getName(): String {
        return AlbumRule.NEW_ALBUM.toString()
    }

    override fun getDescription(): String = "Determines whether current track represents a transition to a new album."

    override fun execute(facts: Facts) {
        albumStateMachine.fire(NewAlbumEvent())
    }

    override fun compareTo(other: Rule): Int = 0

    override fun evaluate(facts: Facts): Boolean {
        val albumState = facts.get<FiniteStateMachine>(AlbumFact.ALBUM_STATE.toString())
        val currentAlbum = facts.get<String>(AlbumFact.CURRENT_ALBUM.toString())
        val nextAlbum = facts.get<String>(AlbumFact.NEXT_ALBUM.toString())

        return AlbumStates.valueOf(albumState.currentState.name) == AlbumStates.EXISTING_ALBUM && currentAlbum != nextAlbum
    }
}