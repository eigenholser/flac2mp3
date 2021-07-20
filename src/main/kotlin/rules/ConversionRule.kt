package com.eigenholser.flac2mp3.rules

import org.jeasy.rules.api.Fact
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule

interface ConversionRule: Rule {
    override fun execute(facts: Facts) = facts.add(Fact(name, true))

    override fun compareTo(other: Rule): Int {
        return when {
            this.priority > other.priority -> 1
            this.priority < other.priority -> -1
            else -> this.name.compareTo(other.name)
        }
    }
}
