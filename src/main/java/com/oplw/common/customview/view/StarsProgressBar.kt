package com.oplw.common.customview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.oplw.common.R
import java.nio.file.Path

/**
 *
 *   @author opLW
 *   @date  2019/12/30
 */
class StarsProgressBar @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attributeSet, defStyleAttr) {

    private var progress = 0f
    private var childViewCount: Int
    private var childViewColor: Int
    private var childClickable = false
    private var childClickListener: ((Int) -> Unit)? = null
    private var pathCreator: PathCreator? = null

    private var isChildAdded = false

    init {
        val typeArray = context.obtainStyledAttributes(attributeSet, R.styleable.StarsProgressBar)
        childViewCount =
            typeArray.getInt(R.styleable.StarsProgressBar_childViewCount, 5)
        childViewColor =
            typeArray.getResourceId(R.styleable.StarsProgressBar_childViewColor, android.R.color.holo_red_light)
        typeArray.recycle()
    }

    fun setChildClickable(clickable: Boolean) {
        childClickable = clickable
    }

    fun setChildClickListener(listener: (Int) -> Unit) {
        childClickable = true
        childClickListener = listener
    }

    fun setProgress(p: Float) {
        progress = p
        invalidate()
    }

    fun setChildViewColor(resId: Int) {
        childViewColor = resId
        invalidate()
    }

    fun setPathCreator(p: PathCreator) {
        pathCreator = p
        invalidate()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (isChildAdded) return

        if (orientation == VERTICAL) {
            layoutChildVertical()
        } else {
            layoutChildHorizontal()
        }
        isChildAdded = true
    }

    private fun layoutChildVertical() {
        val w = getRealWidth()
        val h = getRealHeight() / childViewCount

        for (i in 0 until childViewCount) {
            val childView = ChildView(context, w, h, i)
            addView(childView)
            childView.layout(paddingLeft, paddingTop + h * i - h, paddingLeft + w, paddingTop + h * i)
        }
    }

    private fun layoutChildHorizontal() {
        val w = getRealWidth() / childViewCount
        val h = getRealHeight()

        for (i in 1..childViewCount) {
            val childView = ChildView(context, w, h, i)
            addView(childView)
            childView.layout(paddingLeft + w * i - w, paddingTop, paddingLeft + w * i, paddingTop + h)
        }
    }

    private fun getRealWidth() = width - paddingLeft - paddingRight

    private fun getRealHeight() = height - paddingTop - paddingBottom

    private fun updateProgressBar(position: Int) {
        if (childClickable) {
            childClickListener?.invoke(position)

            progress = position / childViewCount.toFloat()
            invalidate()
        }
    }

    private inner class ChildView(
        context: Context,
        val w: Int, val h: Int,
        val position: Int
    ): View(context) {
        var paint: Paint = Paint()

        init {
            paint.color = Color.TRANSPARENT
            paint.strokeJoin = Paint.Join.ROUND

            setOnClickListener {
                updateProgressBar(position)
            }
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)

            drawBackground(canvas)

            drawShape(canvas)
        }

        private fun drawBackground(canvas: Canvas?) {

        }

        private fun drawShape(canvas: Canvas?) {

        }
    }

    /**
     * Path创造者，决定了每个View的形状。
     * 使用时需假定在100*100的正方形内绘制，事后StarProgressBar会根据实际大小进行映射
     */
    interface PathCreator {
        fun create(): Path
    }
}