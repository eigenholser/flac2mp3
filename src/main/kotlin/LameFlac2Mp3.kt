package com.eigenholser.flac2mp3

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi

object LameFlac2Mp3 {
    // TODO: Make this configurable
    val lameCmd = Config.lamePath

    @ExperimentalPathApi
    fun flac2mp3(flacSrc: String, mp3Dest: String, mp3AlbumPathAbsolute: Path): Unit {
        ProcessBuilder(lameCmd, "-b", "${Config.bitRate}", "-q", "${Config.quality}", flacSrc, mp3Dest)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
    }
}