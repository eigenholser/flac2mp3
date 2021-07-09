package com.eigenholser.flac2mp3.states

import org.jeasy.states.api.AbstractEvent
import org.jeasy.states.api.EventHandler

class SwitchAlbum: EventHandler<AbstractEvent> {
    override fun handleEvent(event: AbstractEvent?) {
        println("SwitchAlbum: Notified of event: {}".format(event?.name))
    }
}