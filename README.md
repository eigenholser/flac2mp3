# flac2mp3

Scan FLAC library and convert new tracks to MP3 with metadata.


## Build and Run

    mvn compile assembly:single
    java -jar target/flac2mp3-1.0-SNAPSHOT-jar-with-dependencies.jar


## Configuration

Create a configuration file with the name `flac2mp3.conf` in
your $HOME directory:

    flacdb.filename = /home/sover/flac.db
    flac_root = /srv/data/music/flac
    mp3_root = /srv/data/music/mp3
    album_art.resolution.thumb = 200
    album_art.resolution.cover = 1000
    album_art.name = album_art.png
    mp3.bitrate = 192
    mp3.quality = 0
    lame.path = /usr/bin/lame

## References
* [JAudioTagger Documentation](http://www.jthink.net/jaudiotagger/examples_read.jsp)
* [JAudioTagger Source](https://bitbucket.org/ijabz/jaudiotagger/src/master/README.md)
* [MP3agic Source](https://github.com/mpatric/mp3agic)
* [kotlin-argparser](https://www.kotlinresources.com/library/kotlin-argparser/)
