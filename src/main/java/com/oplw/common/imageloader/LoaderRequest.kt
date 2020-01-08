package com.oplw.common.imageloader

import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import com.oplw.common.util.encodeMd5
import java.lang.ref.SoftReference

/**
 *   封装一个图片请求
 *   @author opLW
 *   @date  2019/7/11
 */
class LoaderRequest(val context: Context) {
    enum class RequestType {
        URL,
        DRAWABLE
    }

    enum class CacheStrategy {
        ONLY_RAM,
        BOTH
    }

    /**
     * 判断是请求url资源还是drawable文件夹下的内容
     */
    lateinit var type: RequestType
        private set

    /**
     * 缓存的策略
     */
    var strategy = CacheStrategy.ONLY_RAM

    lateinit var url: String

    var resId: Int = -1

    var placeHolder: Int = -1

    var listener: RequestListener? = null

    lateinit var urlMD5: String

    private lateinit var softReference: SoftReference<ImageView>

    fun setCacheStrategy(strategy: CacheStrategy): LoaderRequest {
        this.strategy = strategy
        return this
    }

    fun load(url: String): LoaderRequest {
        type = RequestType.URL
        this.url = url
        this.urlMD5 = encodeMd5(url)!!
        return this
    }

    fun load(resId: Int): LoaderRequest {
        type = RequestType.DRAWABLE
        this.resId = resId
        return this
    }

    fun placeHolder(resID: Int): LoaderRequest {
        this.placeHolder = resID
        return this
    }

    fun addListener(listener: RequestListener): LoaderRequest {
        this.listener = listener
        return this
    }

    fun into(imageView: ImageView) {
        imageView.tag = urlMD5
        softReference = SoftReference(imageView)

        showPlaceHolder(imageView)
        RequestsManager.getInstance(context).bindRequest(this)
    }

    fun getTarget(): ImageView? {
        return softReference.get()
    }


    private fun showPlaceHolder(imageView: ImageView) {
        if (placeHolder > 0) {
            imageView.setImageResource(placeHolder)
        } else {
            imageView.setBackgroundColor(Color.WHITE)
        }
    }
}

