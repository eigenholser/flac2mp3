package com.eigenholser.flac2mp3

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

object DbSettings {
    val db by lazy {
        val filename = File(Config.flacDb).absolutePath
        val url = "jdbc:sqlite:$filename"
        Database.connect(url, "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }
}

object Flac : Table() {
    val id = integer("id").autoIncrement()
    val flacfile = varchar("flacfile", length = 1024)
    val cddbid = varchar("cddbid", length = 32)
    val track = varchar("track", length = 2)
    val fsize = long("fsize")
    val mtime = long("mtime")

    override val primaryKey = PrimaryKey(id, name = "PK_Flac_ID")

    init {
        uniqueIndex("uniqueConstraint_cddbid_track", cddbid, track)
    }
}

data class FlacRow(
    val flacfile: String, val cddbId: String, val track: String,
    val fsize: Long, val mtime: Long
)

object FlacDatabase {
    fun createDatabase(): Unit {
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Flac)
        }
    }

    fun insertFlac(flacfile: String, cddbId: String, track: String, fsize: Long, mtime: Long): Unit {
        transaction {
            addLogger(StdOutSqlLogger)
            Flac.insert { it ->
                it[Flac.flacfile] = flacfile
                it[Flac.cddbid] = cddbId
                it[Flac.track] = track
                it[Flac.fsize] = fsize
                it[Flac.mtime] = mtime
            }
        }
    }

    fun getByCddbAndTrack(cddb: String, track: String): FlacRow {
        val row = transaction {
            val query = Flac.select { Flac.cddbid.eq(cddb) and Flac.track.eq(track) }.withDistinct()
            query.map { it }.first()
        }
        return FlacRow(row[Flac.flacfile], row[Flac.cddbid], row[Flac.track], row[Flac.fsize], row[Flac.mtime])
    }

    fun getAllFlacRows(): List<ResultRow> {
        return transaction {
            addLogger(StdOutSqlLogger)
            Flac.selectAll()
                .orderBy(Flac.cddbid to SortOrder.ASC)
                .orderBy(Flac.track to SortOrder.ASC)
                .toList()
        }
    }
}