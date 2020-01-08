package com.oplw.common.imageloader

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.BufferedInputStream
import java.io.FileDescriptor

/**
 *
 *   @author opLW
 *   @date  2019/7/20
 */
class ImageDecoder {
    private val TAG = "ImageDecoder"

    fun decodeBitmapFromResource(
        res: Resources,
        resId: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, options)

        options.inSampleSize = calculateSampleSize(options, reqWidth, reqHeight)

        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(res, resId, options)
    }

    fun decodeBitmapFromFD(fd: FileDescriptor, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFileDescriptor(fd, null, options)

        options.inSampleSize = calculateSampleSize(options, reqWidth, reqHeight)

        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFileDescriptor(fd, null, options)
    }

    fun decodeBitmapFromStream(
        bufferedIns: BufferedInputStream,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        bufferedIns.mark(1024 * 1024)
        BitmapFactory.decodeStream(bufferedIns, null, options)
        bufferedIns.reset()

        options.also {
            it.inSampleSize = calculateSampleSize(options, reqWidth, reqHeight)
            it.inJustDecodeBounds = false
        }

        return BitmapFactory.decodeStream(bufferedIns, null, options)
    }

    private fun calculateSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        if (reqHeight == 0 || reqWidth == 0) {
            return 1
        }

        var sampleSize = 1
        if (options.outWidth > reqWidth || options.outHeight > reqHeight) {
            val halfWidth = reqWidth / 2
            val halfHeight = reqHeight / 2
            //Log.i(TAG, "$reqWidth $reqHeight")
            //Log.i(TAG, "${options.outWidth} ${options.outHeight}")

            while (halfWidth / sampleSize >= reqWidth && halfHeight / sampleSize >= reqHeight) {
                sampleSize++
            }
            //Log.i(TAG, "sampleSize: $sampleSize")
        }
        return sampleSize
    }
}