package com.eigenholser.flac2mp3

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.File

data class FlacTags(val artist: String, val album: String, val title: String,
    val year: String, val genre: String, val track: String, val cddb: String
)

object Tag {
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

    fun writeMp3Tags(mp3File: String, albumArtFile: String, flacTags: FlacTags): Unit {
        val albumArt = StandardArtwork.createArtworkFromFile(File("$albumArtFile/cover.jpg"))
        val f = AudioFileIO.read(File(mp3File))
        f.tag = ID3v23Tag()
        val tag = f.tag
        tag.addField(albumArt)
        tag.setField(FieldKey.ARTIST, flacTags.artist)
        tag.setField(FieldKey.ALBUM, flacTags.album)
        tag.setField(FieldKey.TITLE, flacTags.title)
        tag.setField(FieldKey.YEAR, flacTags.year)
        tag.setField(FieldKey.GENRE, flacTags.genre)
        tag.setField(FieldKey.TRACK, flacTags.track)
//        tag.createField(FieldKey.valueOf("CDDB"), flacTags.cddb)
        f.commit()
    }
}