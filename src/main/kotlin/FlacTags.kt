package com.eigenholser.flac2mp3

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

data class FlacTags(val artist: String, val album: String, val title: String,
    val year: String, val genre: String, val track: String, val cddb: String
)

object FlacTag {
    fun readFlacTags(flacFile: String): FlacTags {
        val f = AudioFileIO.read(File(flacFile))
        val tag = f.tag
        val artist = tag.getFirst(FieldKey.ARTIST)
        val album = tag.getFirst(FieldKey.ALBUM)
        val title = tag.getFirst(FieldKey.TITLE)
        val year = tag.getFirst(FieldKey.YEAR)
        val genre = tag.getFirst(FieldKey.GENRE)
        val track = tag.getFirst(FieldKey.TRACK)
        val cddb = tag.getFirst("CDDB")
        val tags = FlacTags(artist, album, title, year, genre, track, cddb)

        return tags
    }
}