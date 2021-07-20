package com.eigenholser.flac2mp3.rules

enum class AlbumRule {
    NEW_ALBUM
}

enum class AlbumArtRules {
    NEW_MP3_ART_EXISTS,
    MP3_TAGGED_ART_UPDATED
}

enum class AlbumArtFacts {
    TRACK_DATA, ALBUM_STATE
}
enum class AlbumFact {
    ALBUM_STATE, CURRENT_ALBUM, NEXT_ALBUM
}

object ConversionLib {
}