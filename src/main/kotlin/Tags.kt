package com.eigenholser.flac2mp3

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.File
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException
import java.util.logging.Logger
import java.math.BigInteger
import java.security.MessageDigest

data class FlacTags(
    val artist: String, val album: String, val title: String,
    val year: String, val genre: String, val track: String, val cddb: String
)

fun md5(input:String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
}

object Tag {
    fun readFlacTags(flacFile: String): FlacTags {

        val f = AudioFileIO.read(File(flacFile))
        val tag = f.tag
        val artist = tag.getFirst(FieldKey.ARTIST)
        val album = tag.getFirst(FieldKey.ALBUM)
        val title = tag.getFirst(FieldKey.TITLE)
        val year = if (tag.getFirst(FieldKey.YEAR) == "") {
            "0000"
        } else {
            tag.getFirst(FieldKey.YEAR)
        }
        val genre = if (tag.getFirst(FieldKey.GENRE) == "") {
            "None"
        } else {
            tag.getFirst(FieldKey.GENRE)
        }
        val track = tag.getFirst(FieldKey.TRACK)
        val cddb = if (tag.getFirst("CDDB") == "") {
            if (tag.getFirst("MD5 SIGNATURE") == "") {
                md5(title)
            } else {
                tag.getFirst("MD5 SIGNATURE")
            }
        } else {
            tag.getFirst("CDDB")
        }
        val tags = FlacTags(artist, album, title, year, genre, track, cddb)

        return tags
    }

    fun writeMp3Tags(mp3File: String, albumArtFile: String, flacTags: FlacTags): Unit {
        val f = AudioFileIO.read(File(mp3File))
        f.tag = ID3v23Tag()
        val tag = f.tag
        try {
            val albumArt = StandardArtwork.createArtworkFromFile(File("$albumArtFile/cover.jpg"))
            tag.addField(albumArt)
        } catch (e: FileNotFoundException) {
            Logger.getLogger("Tags Warning: ")
                .warning("Bad file path: File ".plus("$albumArtFile/cover.jpg").plus(" not found"))
        }
        tag.setField(FieldKey.ARTIST, flacTags.artist)
        tag.setField(FieldKey.ALBUM, flacTags.album)
        tag.setField(FieldKey.TITLE, flacTags.title)
        tag.setField(FieldKey.YEAR, flacTags.year)
        tag.setField(FieldKey.GENRE, flacTags.genre)
        tag.setField(FieldKey.TRACK, flacTags.track)
        // TODO: How does this work?
//        tag.createField(FieldKey.valueOf("CDDB"), flacTags.cddb)
        f.commit()
    }
}