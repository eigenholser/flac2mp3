package com.eigenholser.flac2mp3

import ij.IJ
import ij.process.ImageProcessor
import org.jetbrains.exposed.sql.exists
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Logger
import kotlin.math.nextUp

enum class DestType {
    COVER, THUMB
}

object ImageScaler {
    // TODO: Add to config?
    val coverFilename = "cover.jpg"
    val thumbFilename = "thumb.jpg"
    val destFormat = "jpg"

    private fun computeScaleFactor(xAxis: Int, srcSize: Int): Double {
        return (xAxis/srcSize.toDouble())
    }

    private fun makeThumb(ip: ImageProcessor): ImageProcessor {
        val scaleFactor = computeScaleFactor(Config.thumbnailResolution, ip.width)
        return ip.resize(Config.thumbnailResolution, (scaleFactor*ip.height).nextUp().toInt())
    }

    private fun makeCover(ip: ImageProcessor): ImageProcessor {
        val scaleFactor = computeScaleFactor(Config.coverResolution, ip.width)
        return ip.resize(Config.coverResolution, ((scaleFactor*ip.height).nextUp().toInt()))
    }

    fun scaleImage(src: String, dest: String) {
        try {
            val imp = IJ.openImage("$src/album_art.png")
            val ip = imp.processor

            imp.processor = makeThumb(ip)
            IJ.saveAs(imp, destFormat, "$dest/$thumbFilename")

            imp.processor = makeCover(ip)
            IJ.saveAs(imp, destFormat, "$dest/$coverFilename")
        }catch (e: Throwable){
            Logger.getLogger("ImageScaler Warning: ").warning("Bad file path: File ".plus("$src/album_art.png").plus(" not found"))
        }

    }

    fun scaleImage(src: String, dest: String, destType: DestType) {
        try {
            val imp = IJ.openImage("$src/${Config.albumArtFile}")
            val ip = imp.processor

            if (destType == DestType.THUMB) {
                imp.processor = makeThumb(ip)
                IJ.saveAs(imp, destFormat, dest.plus("$dest/$thumbFilename"))
            } else if (destType == DestType.COVER) {
                imp.processor = makeCover(ip)
                IJ.saveAs(imp, destFormat, dest.plus("$dest/$coverFilename"))
            }
        }catch (e: Throwable){
            Logger.getLogger("ImageScaler Warning: ").warning("Bad file path: File ".plus("$src/album_art.png").plus(" not found"))
        }
    }
}