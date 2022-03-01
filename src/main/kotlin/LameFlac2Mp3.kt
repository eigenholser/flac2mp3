package com.eigenholser.flac2mp3

import kotlin.io.path.ExperimentalPathApi

object LameFlac2Mp3 {
    @ExperimentalPathApi
    fun flac2mp3(flacSrc: String, mp3Dest: String): Int {
        return ProcessBuilder(Config.lamePath, "-b", "${Config.bitRate}", "-q", "${Config.quality}", flacSrc, mp3Dest)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
    }
}