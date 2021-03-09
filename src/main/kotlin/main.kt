package com.eigenholser.flac2mp3

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.sqlite.SQLiteDataSource
import java.io.File
import java.nio.file.Files
import java.sql.Connection
import kotlin.io.path.ExperimentalPathApi

object Flac : Table() {
//    val id = integer("id").autoIncrement()
    val flacFile = varchar("flacfile", length=1024)
    val cddbId = varchar("cddbid", length=20)
    val fsize = long("fsize")
    val mtime = long("mtime")

}

@ExperimentalPathApi
fun main(args: Array<String>) {
    val configFile = System.getenv("HOME") + "/flac2mp3.properties"
    println("Reading from $configFile")

    val filename = File("flac.db").absolutePath
    val ds = SQLiteDataSource()
    ds.url = "jdbc:sqlite:$filename"
    val db = Database.connect(ds)
    db.transactionManager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    val config = ConfigFactory.parseFile(File(configFile))
    val mp3Root = config.extract<String>("mp3_root")
    println(mp3Root)
    val flacRoot = config.extract<String>("flac_root")
    println(flacRoot)
    val albumArtFile = config.extract<String>("album_art.name")
    println(albumArtFile)
    val coverResolution = config.extract<Int>("album_art.resolution.cover")
    println(coverResolution)
    val thumbnailResolution = config.extract<Int>("album_art.resolution.thumb")
    println(thumbnailResolution)

    transaction {
        addLogger(StdOutSqlLogger)

        //SchemaUtils.create(Flac)

        File(mp3Root).walk().filter {
            it.extension == "flac"
        }.forEach { file ->
            println("flacfile: $file")
            print("fsize: ${Files.getAttribute(file.toPath(), "size")}  ")
            println("mtime: ${Files.getAttribute(file.toPath(), "lastModifiedTime")}")
            val flacfile = file.absolutePath
            val fSize = Files.getAttribute(file.toPath(), "size")
            val mTime = Files.getAttribute(file.toPath(), "lastModifiedTime")
            Flac.insert { it ->
                it[flacFile] = flacfile
                it[fsize] = fSize as Long
                it[mtime] = 1L
                it[cddbId] = "1"
            }
        }
    }
}

