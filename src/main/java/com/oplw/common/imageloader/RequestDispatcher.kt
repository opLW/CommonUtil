package com.oplw.common.imageloader

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.oplw.common.imageloader.LoaderRequest.RequestType
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 *
 *   @author opLW
 *   @date  2019/7/11
 */

class RequestDispatcher(
    private val request: LoaderRequest,
    private val manager: RequestsManager
) : Runnable {
    private val tag = "RequestDispatcher"
    private val handler = Handler(Looper.getMainLooper())
    private val clipper = manager.imageClipper

    override fun run() {
        loadAndShowBitmap()
    }

    private fun loadAndShowBitmap() {
        when (request.type) {
            RequestType.DRAWABLE -> loadAndShowDrawableRes()
            else -> loadUrlResAndShow()
        }
    }

    private fun showBitmap(bitmap: Bitmap) {
        val target = request.getTarget()
        if (target != null && request.urlMD5 == target.tag) {
            handler.post {
                Log.i(tag, "set bitmap to imageView success")
                target.setImageBitmap(bitmap)
            }
        }
    }

    private fun loadAndShowDrawableRes() {
        if (request.resId > 0) {
            handler.post {
                val target = request.getTarget()
                target?.let {
                    it.setImageResource(request.resId)
                }
            }
        }
    }

    private fun loadUrlResAndShow() {
        var bitmap: Bitmap? = manager.getBitmapFromMenCache(request.urlMD5)
        if (bitmap != null) {
            showBitmap(bitmap)
            return
        }

        val target = request.getTarget()
        bitmap = manager.getBitmapFromDiskCache(request.urlMD5, target!!.width, target!!.height)
        if (bitmap != null) {
            showBitmap(bitmap)
            return
        }

        bitmap = when {
            request.url.contains("http") -> getBitmapFromHttp()
            else -> loadFromExternalMem()
        }
        if (bitmap != null) {
            // 注意点：先showBitmap(bitmap)，后再进行压缩和缓存
            showBitmap(bitmap)
            manager.addBitmapToMemCache(bitmap, request.urlMD5)
            modifyAndAddToDiskCache(bitmap)
        }
    }

    private fun getBitmapFromHttp(): Bitmap? {
        var urlConnection: HttpURLConnection? = null
        var bufferedIns: BufferedInputStream? = null
        try {
            val url = URL(request.url)
            urlConnection = url.openConnection() as HttpURLConnection
            bufferedIns = BufferedInputStream(urlConnection.inputStream, 8 * 1024)

            val target = request.getTarget() ?: return null
            return clipper.decodeBitmapFromStream(bufferedIns, target.width, target.height)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            urlConnection?.disconnect()
            bufferedIns?.close()
        }
        return null
    }

    private fun loadFromExternalMem(): Bitmap? {
        throw Exception("load from External is not work now")
    }

    private fun modifyAndAddToDiskCache(bitmap: Bitmap) {

        val inputStream = modifyBitmapFormat(bitmap)

        addBitmapToCache(inputStream)
    }

    private fun modifyBitmapFormat(bitmap: Bitmap): ByteArrayInputStream{
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 50, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }

    /**
     * 将经过大小裁剪丶质量压缩后产生的流，添加到内存和硬盘缓存中
     */
    private fun addBitmapToCache(inputStream: ByteArrayInputStream) {
        manager.addBitmapToDiskCache(inputStream, request.urlMD5)
        inputStream.close()
    }
}
