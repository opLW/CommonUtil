package com.oplw.common.imageloader

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import android.os.StatFs
import android.util.Log
import android.util.LruCache
import com.jakewharton.disklrucache.DiskLruCache
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 *
 *   @author opLW
 *   @date  2019/7/11
 */


class RequestsManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: RequestsManager? = null

        fun getInstance(context: Context): RequestsManager {
            return instance ?: synchronized(this) {
                instance ?: RequestsManager(context).also { instance = it }
            }
        }
    }

    private val tag = "RequestManager"

    private val cpuCount = Runtime.getRuntime().availableProcessors()
    private val maxCoreThread = cpuCount + 1
    private val maxThread = cpuCount * 2 + 1
    private val keepAliveTime = 10L
    private val factory = object : ThreadFactory {
        val atomicInteger = AtomicInteger(0)

        override fun newThread(r: Runnable?): Thread {
            return Thread(r, "${atomicInteger.getAndIncrement()}")
        }
    }
    private val mThreadPool = ThreadPoolExecutor(
        maxCoreThread,
        maxThread,
        keepAliveTime, TimeUnit.SECONDS,
        LinkedBlockingDeque<Runnable>(),
        factory
    )

    private val diskCacheSize = 1024 * 1024 * 50L
    private val memCacheSize = Runtime.getRuntime().maxMemory().toInt() / 1024 / 8
    private val lruCache = object : LruCache<String, Bitmap>(memCacheSize) {
        override fun sizeOf(key: String?, value: Bitmap?): Int {
            val size = value?.byteCount ?: 0
            return size / 1024
        }
    }
    private lateinit var diskLruCache: DiskLruCache
    private var isDiskCacheCreated = false

    val imageClipper = ImageDecoder()

    init {
        val diskCacheDir = File(context.cacheDir, "opLW_cache")
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
        if (getUsableSpace(diskCacheDir) > diskCacheSize) {
            try {
                diskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, diskCacheSize)
                isDiskCacheCreated = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun bindRequest(request: LoaderRequest) {
        val requestDispatcher = RequestDispatcher(request, this)
        mThreadPool.execute(requestDispatcher)
    }

    private fun getUsableSpace(diskDir: File): Long {
        val stats = StatFs(diskDir.path)
        return stats.blockSizeLong * stats.availableBlocksLong
    }

    fun addBitmapToMemCache(bitmap: Bitmap?, key: String) {
        if (getBitmapFromMenCache(key) == null) {
            lruCache.put(key, bitmap)
        }
    }

    fun getBitmapFromMenCache(key: String): Bitmap? {
        val bitmap = lruCache.get(key)
        if (bitmap != null) {
            Log.i(tag, "load from mem success")
        }
        return bitmap
    }

    fun addBitmapToDiskCache(inputStream: ByteArrayInputStream, key: String): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(tag, "get bitmap in UI thread is not recommended!")
        }
        if (!isDiskCacheCreated) {
            return false
        }

        val editor = diskLruCache.edit(key)
        if (editor != null) {
            val os = editor.newOutputStream(0)
            var b: Int
            while (true) {
                b = inputStream.read()
                if (-1 == b) {
                    break
                }
                os.write(b)
            }
            editor.commit()
            Log.i(tag, "addToDiskSuccess")
            return true
        }
        Log.i(tag, "addToDiskFailed")
        return false
    }

    fun getBitmapFromDiskCache(key: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(tag, "get bitmap in UI thread is not recommended!")
        }
        if (!isDiskCacheCreated) {
            return null
        }

        var bitmap: Bitmap? = null
        val snapShot = diskLruCache.get(key)
        if (snapShot != null) {
            val fis = snapShot.getInputStream(0) as FileInputStream
            bitmap = imageClipper.decodeBitmapFromFD(fis.fd, reqWidth, reqHeight)
            if (bitmap != null) {
                Log.i(tag, "loadFromDiskSuccess")
                addBitmapToMemCache(bitmap, key)
            }
            return bitmap
        }
        Log.i(tag, "loadFromDiskFailed")
        return bitmap
    }
}

