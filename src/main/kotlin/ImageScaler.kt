package com.eigenholser.flac2mp3

import ij.IJ
import ij.process.ImageProcessor
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
        val imp = IJ.openImage("$src/album_art.png")
        val ip = imp.processor

        imp.processor = makeThumb(ip)
        IJ.saveAs(imp, destFormat, "$dest/$thumbFilename")

        imp.processor = makeCover(ip)
        IJ.saveAs(imp, destFormat, "$dest/$coverFilename")
    }

    fun scaleImage(src: String, dest: String, destType: DestType) {
        val imp = IJ.openImage("$src/${Config.albumArtFile}")
        val ip = imp.processor

        if (destType == DestType.THUMB) {
            imp.processor = makeThumb(ip)
            IJ.saveAs(imp, destFormat, dest.plus("$dest/$thumbFilename"))
        } else if (destType == DestType.COVER) {
            imp.processor = makeCover(ip)
            IJ.saveAs(imp, destFormat, dest.plus("$dest/$coverFilename"))
        }
    }
}