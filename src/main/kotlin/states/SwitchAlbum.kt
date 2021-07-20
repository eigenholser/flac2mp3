package com.eigenholser.flac2mp3.states

import org.jeasy.states.api.AbstractEvent
import org.jeasy.states.api.EventHandler
import java.util.logging.Logger

class SwitchAlbum: EventHandler<AbstractEvent> {
    override fun handleEvent(event: AbstractEvent?) {
        logger.info("SwitchAlbum: Notified of event: ${event?.name}")
    }

    companion object {
        private val logger: Logger = Logger.getLogger("SwitchAlbum")
    }
}

