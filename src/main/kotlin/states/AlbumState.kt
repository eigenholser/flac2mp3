package com.eigenholser.flac2mp3.states

import org.jeasy.states.api.AbstractEvent
import org.jeasy.states.api.State
import org.jeasy.states.core.FiniteStateMachineBuilder
import org.jeasy.states.core.TransitionBuilder
import java.nio.file.Path

enum class AlbumEvent {
    NEW_ALBUM_EVENT, EXISTING_ALBUM_EVENT
}

enum class AlbumStates {
    NEW_ALBUM,
    EXISTING_ALBUM
}

data class ConversionState(var nextAlbum: String = "", var prevMp3AlbumPath: Path? = null)
class NewAlbumEvent : AbstractEvent(AlbumEvent.NEW_ALBUM_EVENT.toString())
class ExistingAlbumEvent : AbstractEvent(AlbumEvent.EXISTING_ALBUM_EVENT.toString())

object AlbumState {
    val state = ConversionState()
    val newAlbum = State(AlbumStates.NEW_ALBUM.toString())
    val existingAlbum = State(AlbumStates.EXISTING_ALBUM.toString())
    val states = mutableSetOf(newAlbum, existingAlbum)

    val newAlbumTx = TransitionBuilder()
        .name(AlbumStates.NEW_ALBUM.toString())
        .sourceState(existingAlbum)
        .eventType(NewAlbumEvent::class.java)
        .eventHandler(SwitchAlbum())
        .targetState(newAlbum)
        .build()

    val existingAlbumTx = TransitionBuilder()
        .name(AlbumStates.EXISTING_ALBUM.toString())
        .sourceState(newAlbum)
        .eventType(ExistingAlbumEvent::class.java)
        .eventHandler(SwitchAlbum())
        .targetState(existingAlbum)
        .build()

    val albumStateMachine = FiniteStateMachineBuilder(states, existingAlbum)
        .registerTransition(newAlbumTx)
        .registerTransition(existingAlbumTx)
        .build()
}