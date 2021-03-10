package com.eigenholser.flac2mp3

import ij.IJ
import ij.ImagePlus
import ij.process.ImageProcessor

enum class DestType {
    COVER, THUMB
}

fun computeScaleFactor(xAxis: Int, srcSize: Int): Double {
    return (xAxis/srcSize).toDouble()
}

/*fun getImageProcessor(src: String): MyImage {
    val imp = IJ.openImage(src)
    val ip = imp.getProcessor()

    return MyImage(imp, ip)
}*/


fun scaleImage(src: String, destType: DestType, dest: String) {
    var res = 0
    if (destType == DestType.THUMB) {
        res = 200
    } else if (destType == DestType.COVER) {
        res = 1000
    }
    val imp = IJ.openImage(src)
    val ip = imp.getProcessor()
    val scaleFactor = computeScaleFactor(res, ip.width)

    ip.scale(scaleFactor, scaleFactor)
    imp.setProcessor(ip)
    IJ.saveAs(imp, "jpg", dest)
}