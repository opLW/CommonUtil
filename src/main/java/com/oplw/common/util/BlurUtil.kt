package com.oplw.common.util

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.graphics.drawable.VectorDrawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import androidx.core.view.ViewCompat.setBackground


/**
 *
 *   @author opLW
 *   @date  2019/11/27
 */

class BlurUtil {
    private var mContext: Context? = null
    private var mContainer: View? = null
    private var mRadius: Float = 5f

    private var mImageId: Int? = null
    private var mBitmap: Bitmap? = null
    private var mSvgId: Int? = null

    /**
     * 记录最后添加资源的类型，后来者将覆盖前者
     * imageType 代表 mImageId
     * bitmapType 代表 mBitmap
     * svgType 代表 mSvgId
     */
    private var lastResType = -1
    private val imageType = 1
    private val bitmapType = 2
    private val svgType = 3

    fun setContext(context: Context): BlurUtil {
        mContext = context
        return this
    }

    fun setTargetView(view: View): BlurUtil {
        mContainer = view
        return this
    }

    fun setBlurRadius(radius: Float): BlurUtil {
        mRadius = radius
        return this
    }

    /**
     * 通过此方法设置的资源id，不能为".svg"的图片，否则处理不了
     */
    fun setImageResources(id: Int): BlurUtil {
        lastResType = imageType
        mImageId = id
        return this
    }

    fun setImageBitmap(bitmap: Bitmap): BlurUtil {
        lastResType = bitmapType
        mBitmap = bitmap
        return this
    }

    fun setImageDrawable(drawable: Drawable): BlurUtil {
        lastResType = bitmapType
        mBitmap = drawable2Bitmap(drawable)
        return this
    }

    fun setSvgResources(id: Int): BlurUtil {
        lastResType = svgType
        mSvgId = id
        return this
    }

    fun build() {
        if (mContext == null) {
            throw Exception("mContext must not be null")
        }
        if (mContainer == null && mContainer !is View) {
            throw Exception("mContainer must be a nonullable view")
        }

        applyBlur()
    }

    private fun applyBlur() {
        when (lastResType) {
            imageType -> blurImage()
            svgType -> blurSvg()
            bitmapType -> blur(mBitmap!!)
            else -> throw Exception("no resources has been set")
        }
    }

    private fun blurImage() {
        val bitmap = BitmapFactory.decodeResource(mContext!!.resources, mImageId!!)
        blur(bitmap)
    }

    private fun blurSvg() {
        val res = mContext!!.resources
        val xmlPullParser = res.getXml(mSvgId!!)
        val vectorDrawable = VectorDrawable.createFromXml(res, xmlPullParser)
        val bitmap = drawable2Bitmap(vectorDrawable)
        blur(bitmap!!)
    }

    private fun blur(bitmap: Bitmap) {
        var bkg = bitmap
        val radius: Float = if (mRadius < 25f && mRadius > 0f) {
            mRadius
        } else {
            10f
        }
        bkg = small(bkg)
        var b = bkg.copy(bkg.config, true)

        val rs = RenderScript.create(mContext)
        val input = Allocation.createFromBitmap(
            rs, bkg, Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(radius)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(b)

        b = big(b)
        setBackground(mContainer!!, BitmapDrawable(mContext!!.resources, b))
        rs.destroy()
    }

    private fun small(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postScale(0.25f, 0.25f) //长和宽放大缩小的比例
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun big(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postScale(2f, 2f) //长和宽放大缩小的比例
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun drawable2Bitmap(drawable: Drawable?): Bitmap? {
        return when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            is NinePatchDrawable -> createBitmapFromNinePatch(drawable)
            is VectorDrawable -> createBitmapFromVector(drawable)
            else -> throw Exception("Drawable type can not be identify")
        }
    }

    private fun createBitmapFromNinePatch(drawable: NinePatchDrawable): Bitmap {
        val bitmapConfig = if (drawable.opacity != PixelFormat.OPAQUE) {
            Bitmap.Config.ARGB_8888
        } else {
            Bitmap.Config.RGB_565
        }
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            bitmapConfig
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    private fun createBitmapFromVector(drawable: VectorDrawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }
}