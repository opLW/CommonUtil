package com.oplw.common.imageloader

import android.graphics.Bitmap

/**
 *
 *   @author opLW
 *   @date  2019/7/15
 */
interface RequestListener {

    /**
     * 请求成功时回调的接口
     * @param bitmap 返回成功请求的bitmap
     */
    fun onLoadSuccessfully(bitmap: Bitmap)

    /**
     * 请求失败时回调的接口
     */
    fun onLoadFailed()
}