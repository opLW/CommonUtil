package com.oplw.common.customview.layout

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.cardview.widget.CardView
import kotlin.math.abs

// 需前往value/attrs.xml文件中获取相应的styleable，以及引入BlurUtil.kt
import com.oplw.common.R
import com.oplw.common.util.BlurUtil

/**
 *
 *   @author opLW
 *   @date  2019/11/25
 */
class ChainLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attributeSet, defStyleAttr) {
    // 静态布局相关参数
    private val headVisibleHeight: Int
    private val headHideHeight: Int
    private val headBackgroundColor: Int
    private val showMarkAndCircle: Boolean
    private val headBottomMaskColor: Int
    private val headBottomMaskHeight: Int
    /**
     * 取值范围: `-1f, 1f`的闭区间。
     *
     * `-1f, 0`的闭区间代表向右边倾斜，值越小越倾斜
     *
     * `0, 1f`的闭区间代表向左边倾斜，值越大越倾斜
     */
    private var headBottomMaskSlope: Float
    private val circleIvElevation: Int
    private val circleIvMarginB: Int
    private val circleIvMarginH: Int
    private val circleIvSize: Int
    /**
     * scrollView的直接子容器。
     */
    private lateinit var mainContainer: LinearLayout
    /**
     * headBackgroundIv的背景图片需要特殊处理，所以通过函数向外开放。
     */
    private lateinit var headBackgroundIv: ImageView
    /**
     * circleIv的背景图不需要特殊处理，直接向外暴露;
     *
     * **注意**: 当showMarkAndCircle为false时，circleIv不会被初始化。
     */
    lateinit var circleIv: ImageView
        private set

    // 动效相关参数
    private var lastY = 0f
    /**
     * Head当前向上的偏移量
     */
    private var curTranslationY = 0f
    private var isHeadChanged = false
    /**
     * 当隐藏部分剩下多少时，触发图片放大功能
     */
    private val startScaleHiddenHeight: Float
    /**
     * 图片放大时的计算器
     */
    var scaleCalculator: ScaleCalculator
    /**
     * 下拉松开手指时，Head恢复至初始状态的插值器
     */
    var restoreInterpolator: TimeInterpolator
    var restoreDuration: Long
            
    init {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.ChainLayout)
        headVisibleHeight = a.getDimensionPixelSize(R.styleable.ChainLayout_headVisibleHeight, 600)
        headHideHeight = a.getDimensionPixelSize(R.styleable.ChainLayout_headHideHeight, 200)
        headBackgroundColor = a.getColor(R.styleable.ChainLayout_headBackgroundColor, Color.WHITE)
        headBottomMaskColor = a.getColor(R.styleable.ChainLayout_headBottomMaskColor, Color.WHITE)
        headBottomMaskHeight = a.getDimensionPixelSize(R.styleable.ChainLayout_headBottomMaskHeight, 250)
        headBottomMaskSlope = a.getFraction(R.styleable.ChainLayout_headBottomMaskSlope, 1, 1, 0.5f)
        circleIvElevation = a.getDimensionPixelSize(R.styleable.ChainLayout_circleIvElevation, 20)
        circleIvSize = a.getDimensionPixelSize(R.styleable.ChainLayout_circleIvSize, 250)
        circleIvMarginH = a.getDimensionPixelSize(R.styleable.ChainLayout_circleIvMarginH, 80)
        circleIvMarginB = a.getDimensionPixelSize(R.styleable.ChainLayout_circleIvMarginB, 20)
        showMarkAndCircle = a.getBoolean(R.styleable.ChainLayout_showMarkAndCircle, true)
        a.recycle()
        
        startScaleHiddenHeight = headHideHeight / 2f
        scaleCalculator = object: ScaleCalculator{
            override fun calculate(factor: Float): Float {
                // 利用tan函数，使得放大效果逐渐明显，同时除于5避免放大效果太明显
                return Math.tan(factor.toDouble()).toFloat() / 5
            }
        }
        restoreDuration = 500
        restoreInterpolator = OvershootInterpolator()
        
        initMainContainer()
    }

    private fun initMainContainer() {
        mainContainer = LinearLayout(context)
        mainContainer.orientation = LinearLayout.VERTICAL

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // 调用ScrollView的addView方法，添加唯一子View：mainContainer
        super.addView(mainContainer, -1, layoutParams)

        initInherentHead()
    }

    private fun initInherentHead() {
        val layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, headVisibleHeight + headHideHeight)
        val headView = FrameLayout(context)
        headView.layoutParams = layoutParams
        headView.setBackgroundColor(headBackgroundColor)

        addBackgroundImageView(headView)

        addBottomMask(headView)

        addCircleIv(headView)

        mainContainer.addView(headView)
    }

    private fun addBackgroundImageView(headView: FrameLayout) {
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        headBackgroundIv = ImageView(context)
        headBackgroundIv.layoutParams = layoutParams
        headBackgroundIv.scaleType = ImageView.ScaleType.FIT_CENTER

        headView.addView(headBackgroundIv)
    }

    private fun addBottomMask(headView: FrameLayout) {
        if (!showMarkAndCircle) return

        val lp1 = LayoutParams(LayoutParams.MATCH_PARENT, headBottomMaskHeight)
        lp1.gravity = Gravity.BOTTOM
        val mask = MyMask(context)
        mask.layoutParams = lp1
        mask.setBackgroundColor(Color.TRANSPARENT)

        headView.addView(mask)
    }

    private fun addCircleIv(frameLayout: FrameLayout) {
        if (!showMarkAndCircle) return

        val lp2 = LayoutParams(circleIvSize, circleIvSize)
        if (headBottomMaskSlope > 0f) {
            lp2.marginStart = circleIvMarginH
            lp2.gravity = Gravity.BOTTOM.or(Gravity.START)
        } else {
            lp2.marginEnd = circleIvMarginH
            lp2.gravity = Gravity.BOTTOM.or(Gravity.END)
        }
        lp2.bottomMargin = circleIvMarginB

        val cardView = CardView(context)
        with(cardView) {
            layoutParams = lp2
            radius = circleIvSize.toFloat() / 2
            cardElevation = circleIvElevation.toFloat()
        }

        circleIv = ImageView(context)
        cardView.addView(circleIv)

        frameLayout.addView(cardView)
    }

    fun setHBackgroundDrawable(drawable: Drawable, blurRadius: Float = 5f) {
        if (0f == blurRadius) {
            headBackgroundIv.setImageDrawable(drawable)
        } else {
            getBlurUtil(blurRadius).setImageDrawable(drawable).build()
        }
    }

    fun setHBackgroundRes(resId: Int, blurRadius: Float = 5f) {
        if (0f == blurRadius) {
            headBackgroundIv.setImageResource(resId)
        } else {
            getBlurUtil(blurRadius).setImageResources(resId).build()
        }
    }

    fun setHBackgroundSvgRes(resId: Int, blurRadius: Float = 5f) {
        if (0f == blurRadius) {
            headBackgroundIv.setImageResource(resId)
        } else {
            getBlurUtil(blurRadius).setSvgResources(resId).build()
        }
    }

    fun setHBackgroundBitmap(bitmap: Bitmap, blurRadius: Float = 5f) {
        if (0f == blurRadius) {
            headBackgroundIv.setImageBitmap(bitmap)
        } else {
            getBlurUtil(blurRadius).setImageBitmap(bitmap).build()
        }
    }

    private fun getBlurUtil(blurRadius: Float) = BlurUtil()
        .setTargetView(headBackgroundIv)
        .setContext(context)
        .setBlurRadius(blurRadius)

    // 重写一系列的addView方法，将原本添加到ScrollView的子View转而添加到mainContainer中
    override fun addView(child: View?) {
        mainContainer.addView(child)
    }

    override fun addView(child: View?, index: Int) {
        mainContainer.addView(child, index)
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        mainContainer.addView(child, params)
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        mainContainer.addView(child, index, params)
    }

    override fun addView(child: View?, width: Int, height: Int) {
        mainContainer.addView(child, width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // 布局开始时，需让Head部分为默认状态
        changeHeadToDefaultStatus(false)
        super.onLayout(changed, l, t, r, b)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (null == ev) return true

        //ACTION_DOWN首次到来时记录位置
        if (ev.action == MotionEvent.ACTION_DOWN) {
            lastY = ev.y
        }
        
        // 如果前后两次位置超过2f则判定为滑动，将事件拦截 
        val deltaY = ev.y - lastY
        if (Math.abs(deltaY) >= 2f) {
            return true
        }

        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return true

        val deltaY = ev.y - lastY
        lastY = ev.y
        // ( 在默认状态下继续向下拉动 | Head处在变化中 ) 这两种情况需要将滑动事件处理
        if (ev.action == MotionEvent.ACTION_MOVE && (scrollY == 0 && deltaY > 0 || isHeadChanged)) {
            changeHeadStatus(deltaY)
            return true
        }

        // 用户松开手指并且Head被拉动过，此时需要恢复至默认状态。
        if (ev.action == MotionEvent.ACTION_UP && isHeadChanged) {
            changeHeadToDefaultStatus(true)
            return true
        }

        return super.onTouchEvent(ev)
    }

    private fun changeHeadStatus(deltaY: Float) {
        curTranslationY += deltaY
        // 向下拉到了最大值
        if (curTranslationY > 0f) {
            curTranslationY = 0f
            return
        }
        // 向上滑到了初始状态
        if (curTranslationY <= -headHideHeight) {
            changeHeadToDefaultStatus(false)
            return
        }

        mainContainer.translationY = curTranslationY
        // 此时的TranslationY还是负值并且由于向下拉动还在不断向0靠近，直至为0
        if (curTranslationY > -startScaleHiddenHeight) {
            val factor = 1 - (curTranslationY / -startScaleHiddenHeight)
            val scale = scaleCalculator.calculate(factor)
            headBackgroundIv.scaleX = 1 + scale
            headBackgroundIv.scaleY = 1 + scale
        }
        isHeadChanged = true
    }

    /**
     * 将Head设置为默认值，主要将Head的translationY设置为负，达到隐藏部分Head的目的。
     * @param needShowAnimation 是否使用动画，让变化更加自然。
     */
    private fun changeHeadToDefaultStatus(needShowAnimation: Boolean) {
        if (needShowAnimation) {
            makeAnimator()
        } else {
            curTranslationY = -headHideHeight.toFloat()
            mainContainer.translationY = -headHideHeight.toFloat()
            headBackgroundIv.scaleX = 1f
            headBackgroundIv.scaleY = 1f
        }

        isHeadChanged = false
    }

    private fun makeAnimator() {
        val originalScale = headBackgroundIv.scaleX

        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.interpolator = restoreInterpolator
        valueAnimator.duration = restoreDuration
        valueAnimator.addUpdateListener {
            val fraction = it.animatedValue as Float
            mainContainer.translationY =
                curTranslationY + (-headHideHeight - curTranslationY) * fraction
            val scale = originalScale + (1f - originalScale) * fraction
            headBackgroundIv.scaleX = scale
            headBackgroundIv.scaleY = scale
        }
        valueAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}

            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationStart(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                curTranslationY = -headHideHeight.toFloat()
            }
        })
        valueAnimator.start()
    }

    /**
     * 自定义View，重写onDraw方法，实现梯形背景。
     */
    private inner class MyMask(context: Context) : View(context) {
        val paint: Paint = Paint()

        init {
            with(paint) {
                color = headBottomMaskColor
                style = Paint.Style.FILL_AND_STROKE
                isAntiAlias = true
            }

            makeSuitableSlope()
        }

        override fun onDraw(canvas: Canvas?) {
            val leftStartY = getLeftStartY()
            val rightStartY = getRightStartY()
            val path = getPath(leftStartY, rightStartY)

            canvas?.drawPath(path, paint)
        }

        private fun makeSuitableSlope() {
            if (headBottomMaskSlope > 1f) {
                headBottomMaskSlope = 1f
            } else if (headBottomMaskSlope < -1f) {
                headBottomMaskSlope = -1f
            }
        }

        private fun getLeftStartY(): Float {
            return if (headBottomMaskSlope <= 0) {
                0f
            } else {
                headBottomMaskSlope * height
            }
        }

        private fun getRightStartY(): Float {
            return if (headBottomMaskSlope <= 0) {
                abs(headBottomMaskSlope) * height
            } else {
                0f
            }
        }

        private fun getPath(leftStartY: Float, rightStartY: Float): Path {
            val path = Path()
            path.moveTo(0f, leftStartY)
            path.lineTo(0f, height.toFloat())
            path.lineTo(width.toFloat(), height.toFloat())
            path.lineTo(width.toFloat(), rightStartY)
            path.close()
            return path
        }
    }

    interface ScaleCalculator {
        /**
         * 计算放大的倍数。
         *
         * **注意**：由于只提供放大的功能，所以原控件的scaleX和scaleY最小为1。
         * @param factor 值为从0到1
         * @return 返回最终放大的倍数
         */
        fun calculate(factor: Float): Float
    }
