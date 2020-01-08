package com.oplw.common.customview.layout

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 *
 *   @author opLW
 *   @date  2019/7/4
 */
class RotateBanner : ViewGroup {
    companion object {
        /**
         * 当前最多允许多少个child
         */
        const val MAX_COUNT = 4
    }

    private var needInitBaseSize = true

    private var leftBaseX = -1
    private var midBaseX = -1
    private var rightBaseX = -1

    private var topBaseY = -1
    private var midBaseY = -1
    private var bottomBaseY = -1
    private var chosenBaseY = -1

    private val minAlpha = 0.5f
    private val midAlpha = 0.7f
    private val maxAlpha = 1.0f

    private val minScale = 0.3f
    private val midScale = 0.4f
    private val maxScale = 0.9f
    private val chosenScale = 0.95f

    private val mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    /**
     * 一次完整的child顺序交换需要移动的距离
     */
    private var mOffsetOfOneOrderChange = 0f
    /**
     * 在一次滑动过程中，是否发生了child顺序的交换
     */
    private var isOrderChanged = false
    private var isBeingDragged = false
    /**
     * 在一次滑动过程中，手指移动的距离与mOffsetOfOneOrderChange的百分比，手指离开之后清零
     */
    private var mOffsetPercent = 0f
    /**
     * 在一次滑动过程中，手指移动的距离，手指离开之后清零
     */
    private var mOffsetX = 0f
    private var mLastX = 0f
    private var mLastY = 0f
    private var maxHeight = 0
    private var maxWidth = 0

    private lateinit var mScheduler: ScheduledExecutorService

    inner class MyLayoutParams : MarginLayoutParams {
        //记录child的原始位置
        var originalPosition: Int = 0

        //目标位置的参数
        var targetAlpha = 0.0f
        var targetScale = 0.0f
        var targetX = 0
        var targetY = 0

        //原始位置的参数
        var fromAlpha = 0.0f
        var fromScale = 0.0f
        var fromX = 0
        var fromY = 0

        //当前位置的参数
        var currentAlpha = 0.0f
        var currentScale = 0.0f
        var currentX = 0
        var currentY = 0

        constructor(params: LayoutParams?) : super(params)

        constructor(context: Context, attr: AttributeSet?) : super(context, attr)

        constructor(width: Int, height: Int) : super(width, height)
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)

    constructor(context: Context, attr: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attr,
        defStyleAttr
    )

    override fun generateLayoutParams(attrs: AttributeSet?): MyLayoutParams {
        return MyLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams?): MyLayoutParams {
        return MyLayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): MyLayoutParams {
        return MyLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (childCount >= MAX_COUNT) {
            throw RuntimeException("The count of child must below 4!")
        }
        if (null == child) {
            return
        }

        val lp = if (null == params) {
            generateDefaultLayoutParams()
        } else {
            generateLayoutParams(params)
        }
        lp.originalPosition = childCount

        if (childCount == MAX_COUNT - 1) {
            maxHeight = child.measuredHeight
            maxWidth = child.measuredWidth
        }
        super.addView(child, index, lp)
    }

    private fun resetLayoutParams(index: Int) {
        val lp = this[index].layoutParams as MyLayoutParams
        when (index) {
            0 -> {
                lp.fromAlpha = midAlpha
                lp.fromScale = midScale
                lp.fromX = leftBaseX
                lp.fromY = midBaseY
            }
            1 -> {
                lp.fromAlpha = minAlpha
                lp.fromScale = minScale
                lp.fromX = midBaseX
                lp.fromY = topBaseY
            }
            2 -> {
                lp.fromAlpha = midAlpha
                lp.fromScale = midScale
                lp.fromX = rightBaseX
                lp.fromY = midBaseY
            }
            else -> {
                lp.fromAlpha = maxAlpha
                lp.fromScale = maxScale
                lp.fromX = midBaseX
                lp.fromY = bottomBaseY
            }
        }
        lp.targetScale = lp.fromScale
        lp.targetAlpha = lp.fromAlpha
        lp.targetX = lp.fromX
        lp.targetY = lp.fromY
        lp.currentScale = lp.fromScale
        lp.currentAlpha = lp.fromAlpha
        lp.currentX = lp.fromX
        lp.currentY = lp.fromY
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?) = null != ev

    private val theIndexOfTopOne = 1

    private fun handleClickAction(x: Float, y: Float) {
        val hitView = findHitView(x, y)
        if (hitView != null) {
            val index = indexOfChild(hitView)
            if (index == childCount - 1) {
                val lp = hitView.layoutParams as MyLayoutParams
                mListener(lp.originalPosition)
            } else if (index != theIndexOfTopOne) {
                exchangeOrderWhenChildChosen(index)
            }
        }
    }

    private fun findHitView(x: Float, y: Float): View? {
        //从顶层的子View开始递减遍历
        for (index in childCount - 1 downTo 0) {
            val child = this[index]
            //判断触摸点是否在这个子View内
            if (pointInView(child, floatArrayOf(x, y))) {
                //如果在就直接返回它
                return child
            }
        }
        //没有找到，返回null
        return null
    }

    private fun pointInView(view: View, points: FloatArray): Boolean {
        // 像ViewGroup那样，先对齐一下Left和Top
        points[0] -= view.left.toFloat()
        points[1] -= view.top.toFloat()
        // 获取View所对应的矩阵
        val matrix = view.matrix
        // 如果矩阵有应用过变换
        if (!matrix.isIdentity) {
            // 反转矩阵
            matrix.invert(matrix)
            // 映射坐标点
            matrix.mapPoints(points)
        }
        //判断坐标点是否在view范围内
        return points[0] >= 0 && points[1] >= 0 && points[0] < view.width && points[1] < view.height
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (null == event) {
            return false
        }

        val currentX = event.x
        val currentY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mLastY = currentY
                mLastX = currentX
                isBeingDragged = false
                stopAutoRotate()
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = currentX - mLastX
                val deltaY = currentY - mLastY
                if (Math.abs(deltaX) >= mTouchSlop || Math.abs(deltaY) >= mTouchSlop) {
                    mLastX = currentX
                    mLastY = currentY
                    isBeingDragged = true
                }
                mOffsetX += currentX - mLastX
                updateItem()
            }
            MotionEvent.ACTION_UP -> {
                if (isBeingDragged) {
                    mOffsetX =
                        if (Math.abs(mOffsetX) >= mOffsetOfOneOrderChange / 2) mOffsetOfOneOrderChange else 0F
                    updateItem()
                    isBeingDragged = false
                } else {
                    handleClickAction(currentX, currentY)
                }
                startAutoRotate()
            }
        }
        mLastX = currentX
        mLastY = currentY
        return true
    }

    private fun updateItem() {
        mOffsetPercent = mOffsetX / mOffsetOfOneOrderChange
        updateChildOrder()
        updateChildScaleAlphaAndPosition()
        updateChildFromAndTo()
        requestLayout()
    }

    private fun updateChildFromAndTo() {
        val currentPercent = Math.abs(mOffsetPercent)
        if (currentPercent >= 1F || currentPercent == 0F) {
            //完成一次完整的交换,重置所有的数据
            resetDataAfterOneWholeChange()
        } else {
            for (i in 0 until childCount) {
                val fromLp = this[i].layoutParams as MyLayoutParams
                val toLp = this[getToIndex(i)].layoutParams as MyLayoutParams
                fromLp.targetY = toLp.fromY
                fromLp.targetX = toLp.fromX
                fromLp.targetScale = toLp.fromScale
                fromLp.targetAlpha = toLp.fromAlpha
            }
        }
    }

    private fun resetDataAfterOneWholeChange() {
        for (i in 0 until childCount) {
            resetLayoutParams(i)
            if (i == childCount - 1) {
                showChosenAnimation(i)
            }
        }
        isOrderChanged = false
        if (isBeingDragged) mOffsetPercent %= 1F else mOffsetPercent = 0F
        if (isBeingDragged) mOffsetX %= mOffsetOfOneOrderChange else mOffsetX = 0F
    }

    private fun getToIndex(from: Int) = if (mOffsetPercent > 0) {
        (from + childCount - 1) % childCount
    } else {
        (from + 1) % childCount
    }

    private fun updateChildOrder() {
        if (!isOrderChanged && Math.abs(mOffsetPercent) > 0.5F) {
            if (mOffsetPercent > 0) {
                exchangeOrder(false)
            } else {
                exchangeOrder(true)
            }
            isOrderChanged = true
        } else if (isOrderChanged && Math.abs(mOffsetPercent) <= 0.5F) {
            if (mOffsetPercent > 0) {
                exchangeOrder(false)
            } else {
                exchangeOrder(true)
            }
            isOrderChanged = false
        }
    }

    private fun exchangeOrderWhenChildChosen(chosenIndex: Int) {
        var offset = 0F
        if (chosenIndex == 0) {
            offset = mOffsetOfOneOrderChange
        } else if (chosenIndex == 2) {
            offset = -mOffsetOfOneOrderChange
        }
        ValueAnimator
            .ofFloat(0f, offset)
            .apply {
                this.duration = 500
                this.interpolator = AccelerateDecelerateInterpolator()
                this.addUpdateListener {
                    mOffsetX = it.animatedValue as Float
                    updateItem()
                }
                this.start()
            }
    }

    /**
     * 重置child在父布局中的层级关系
     * 只有顺时针和逆时针两种处理情形
     */
    private fun exchangeOrder(isClockWise: Boolean) {
        val childAry = arrayOfNulls<View>(childCount)
        for (i in 0 until childCount) {
            childAry[i] = getChildAt(i)
        }
        detachAllViewsFromParent()
        var start = if (isClockWise) {
            childAry.size - 1
        } else {
            1
        }
        while (childCount < childAry.size) {
            val child = childAry[(start) % childAry.size]
            attachViewToParent(child, -1, child!!.layoutParams)
            start++
        }
    }

    private fun updateChildScaleAlphaAndPosition() {
        for (i in 0 until childCount) {
            val lp = this[i].layoutParams as MyLayoutParams
            lp.currentScale = computingFormula(lp.fromScale, lp.targetScale)
            lp.currentAlpha = computingFormula(lp.fromAlpha, lp.targetAlpha)
            lp.currentX = computingFormula(lp.fromX, lp.targetX).toInt()
            lp.currentY = computingFormula(lp.fromY, lp.targetY).toInt()
        }
    }

    private fun computingFormula(fromValue: Float, toValue: Float) =
        fromValue + Math.abs(mOffsetPercent) * (toValue - fromValue)

    private fun computingFormula(fromValue: Int, toValue: Int) =
        fromValue + Math.abs(mOffsetPercent) * (toValue - fromValue)

    private fun showChosenAnimation(index: Int) {
        val lp = this[index].layoutParams as MyLayoutParams
        lp.currentScale = chosenScale
        lp.currentY = chosenBaseY
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val width = if (widthMode == MeasureSpec.EXACTLY) {
            widthSize
        } else {
            val width = this[0].measuredWidth
            width * 2
        }

        val height = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else {
            val height = this[childCount - 1].measuredHeight
            val displayMetrics = DisplayMetrics()
            display.getMetrics(displayMetrics)
            Math.min(height * 2, displayMetrics.heightPixels / 3)
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            layoutChild(i)
        }
    }

    private fun layoutChild(index: Int) {
        val child = this[index]
        val childWidth = child.measuredWidth
        val childHeight = child.measuredHeight

        val lp = child.layoutParams as MyLayoutParams
        initBaseSize()

        val top = lp.currentY - childHeight / 2
        val bottom = lp.currentY + childHeight / 2
        val right = lp.currentX + childWidth / 2
        val left = lp.currentX - childWidth / 2

        with(child) {
            this.alpha = lp.currentAlpha
            this.scaleX = lp.currentScale
            this.scaleY = lp.currentScale
            this.layout(left, top, right, bottom)
        }
    }

    private fun initBaseSize() {
        if (needInitBaseSize) {
            initBaseX()
            initBaseY()
            for (i in 0 until childCount) {
                resetLayoutParams(i)
                if (i == childCount - 1) {
                    mOffsetOfOneOrderChange = width / 2f
                    showChosenAnimation(i)
                }
            }
            needInitBaseSize = false
        }
    }

    private fun initBaseX() {
        leftBaseX = width / 4
        midBaseX = width / 2
        rightBaseX = width * 3 / 4
    }

    private fun initBaseY() {
        topBaseY = height * 3 / 16
        midBaseY = height * 5 / 16
        bottomBaseY = height * 11 / 16
        chosenBaseY = height * 5 / 8
    }

    private val valueUpdater by lazy {
        ValueAnimator.ofFloat(0f, mOffsetOfOneOrderChange)
            .apply {
                this.duration = 200
                this.addUpdateListener {
                    var value = it.animatedValue as Float
                    if (defaultRotateDirection == RotateDirection.ClockWise) {
                        value = -value
                    }
                    mOffsetX = value
                    updateItem()
                }
            }
    }

    fun startAutoRotate() {
        mScheduler = Executors.newSingleThreadScheduledExecutor()
        mScheduler.scheduleAtFixedRate({
            post {
                valueUpdater.start()
            }
        }, 2000, 4000, TimeUnit.MILLISECONDS)
    }

    fun stopAutoRotate() {
        valueUpdater.cancel()
        mScheduler.shutdownNow()
    }

    enum class RotateDirection {
        ClockWise,
        AntiClockWise
    }

    private var defaultRotateDirection =
        RotateDirection.ClockWise
    fun setRotateDirection(rotateDirection: RotateDirection) {
        defaultRotateDirection = rotateDirection
    }

    private lateinit var mListener: (originalPosition: Int) -> Unit
    fun setChildClickListener(listener: (originalPosition: Int) -> Unit) {
        mListener = listener
    }

    operator fun get(index: Int): View = getChildAt(index)
}
