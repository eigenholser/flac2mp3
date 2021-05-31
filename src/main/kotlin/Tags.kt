package com.eigenholser.flac2mp3

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.File
import java.io.FileNotFoundException
import java.math.BigInteger
import java.security.MessageDigest
import java.util.logging.Logger

data class FlacTags(
    val artist: String, val album: String, val title: String,
    val year: String, val genre: String, val track: String, val cddb: String
)

fun md5(input:String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
}

object Tag {
    val logger = Logger.getLogger("Tags")

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
        return FlacTags(artist, album, title, year, genre, track, cddb)
    }

    fun writeMp3Tags(mp3File: String, mp3AlbumPath: String, flacTags: FlacTags): Unit {
        val f = AudioFileIO.read(File(mp3File))
        f.tag = ID3v24Tag()
        val tag = f.tag
        // TODO: Put this try block in addAlbumArtField()
        try {
            val albumArt = StandardArtwork.createArtworkFromFile(File("$mp3AlbumPath/${Config.coverArtFile}"))
            tag.addField(albumArt)
        } catch (e: FileNotFoundException) {
            ImageScaler.logger.warning("Album art not found: $mp3AlbumPath/${Config.coverArtFile}")
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

    fun getAlbumArt(mp3File: File): Artwork {
        val f = AudioFileIO.read(mp3File)
        val tag = f.tag // TODO: Is this null if the MP3 has not been tagged? try/catch?
        // If this is null there is no artwork?? Prove this hypothesis.
        return tag.firstArtwork
    }

    fun addAlbumArtField() {

    }

    fun deleteAlbumArtField() {

    }

    fun updateAlbumArtField() {
        deleteAlbumArtField()
        addAlbumArtField()
    }
}