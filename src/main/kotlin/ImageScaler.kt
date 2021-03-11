package com.eigenholser.flac2mp3

import ij.IJ
import ij.ImagePlus
import ij.process.ImageProcessor
import kotlin.math.nextUp

enum class DestType {
    COVER, THUMB
}

object ImageScaler {
    val coverFilename = "cover.jpg"
    val thumbFilename = "thumb.jpg"
    val destFormat = "jpg"

    private fun computeScaleFactor(xAxis: Int, srcSize: Int): Double {
        println("${xAxis/srcSize.toDouble()}")
        return (xAxis/srcSize.toDouble())
    }

    private fun makeThumb(ip: ImageProcessor): ImageProcessor {
        //TODO: Get resolution values from config file
        val scaleFactor = computeScaleFactor(200, ip.width)
        return ip.resize(200, (scaleFactor*ip.height).nextUp().toInt())
    }

    private fun makeCover(ip: ImageProcessor): ImageProcessor {
        //TODO: Get resolution values from config file
        val scaleFactor = computeScaleFactor(1000, ip.width)
        return ip.resize(1000, ((scaleFactor*ip.height).nextUp().toInt()))
    }

    fun scaleImage(src: String, dest: String) {
        val imp = IJ.openImage(src)
        val ip = imp.processor

        imp.processor = makeThumb(ip)
        IJ.saveAs(imp, destFormat, dest.plus(thumbFilename))

        imp.processor = makeCover(ip)
        IJ.saveAs(imp, destFormat, dest.plus(coverFilename))
    }

    fun scaleImage(src: String, dest: String, destType: DestType) {
        val imp = IJ.openImage(src)
        val ip = imp.processor

        if (destType == DestType.THUMB) {
            imp.processor = makeThumb(ip)
            IJ.saveAs(imp, destFormat, dest.plus(thumbFilename))
        } else if (destType == DestType.COVER) {
            imp.processor = makeCover(ip)
            IJ.saveAs(imp, destFormat, dest.plus(coverFilename))
        }
    }
}