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
import com.oplw.common.R
import com.oplw.common.base.BaseAnimatorListener
import com.oplw.common.util.BlurUtil
import kotlin.math.abs

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

    private val headVisibleHeight: Int
    private val headHideHeight: Int
    private val headBackgroundColor: Int
    /**
     * 是否需要Head的底部部分
     */
    private val needHeadBottom: Boolean
    private val headBottomMarkColor: Int
    private val headBottomMarkHeight: Int
    /**
     * 取值范围: [-1f, 1f]。
     * [-1f, 0] 代表向右边倾斜，值越小越倾斜
     * [0, 1f] 代表向左边倾斜，值越大越倾斜
     */
    private var headBottomMarkSlope: Float
    private var headBottomIvElevation: Int

    private var lastY = 0f
    private var curTranslationY = 0f
    private var isHeadChanged = false
    private val headBottomIvSize: Int
        get() {
            return headBottomMarkHeight * 15 / 16
        }

    // scrollView的直接子容器
    private lateinit var mainContainer: LinearLayout
    // headBackgroundIv的背景图片需要特殊处理，所以对外隐蔽
    private lateinit var headBackgroundIv: ImageView
    // headBottomIv的背景图不需要特殊处理，直接向外暴露
    lateinit var headBottomIv: ImageView
        private set

    init {
        val typeArray = context.obtainStyledAttributes(attributeSet, R.styleable.ChainLayout)
        headVisibleHeight =
            typeArray.getDimensionPixelSize(R.styleable.ChainLayout_headVisibleHeight, 600)
        headHideHeight =
            typeArray.getDimensionPixelSize(R.styleable.ChainLayout_headHideHeight, 200)
        headBackgroundColor =
            typeArray.getColor(R.styleable.ChainLayout_headBackgroundColor, Color.WHITE)
        needHeadBottom =
            typeArray.getBoolean(R.styleable.ChainLayout_needHeadBottom, true)
        headBottomMarkColor =
            typeArray.getColor(R.styleable.ChainLayout_headBottomMarkColor, Color.WHITE)
        setBackgroundColor(headBottomMarkColor)
        headBottomMarkHeight =
            typeArray.getDimensionPixelSize(R.styleable.ChainLayout_headBottomMarkHeight, 250)
        headBottomMarkSlope =
            typeArray.getFraction(R.styleable.ChainLayout_headBottomMarkSlope, 1, 1, 0.5f)
        makeSuitableSlope()
        headBottomIvElevation =
            typeArray.getDimensionPixelSize(R.styleable.ChainLayout_headBottomIvElevation, 20)
        typeArray.recycle()

        initMainContainer()
    }

    private fun makeSuitableSlope() {
        if (headBottomMarkSlope > 1f) {
            headBottomMarkSlope = 1f
        } else if (headBottomMarkSlope < -1f) {
            headBottomMarkSlope = -1f
        }
    }

    private fun initMainContainer() {
        mainContainer = LinearLayout(context)
        mainContainer.orientation = LinearLayout.VERTICAL

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

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

        addBottomMark(headView)

        mainContainer.addView(headView)
    }

    private fun addBackgroundImageView(headView: FrameLayout) {
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        headBackgroundIv = ImageView(context)
        headBackgroundIv.layoutParams = layoutParams
        headBackgroundIv.scaleType = ImageView.ScaleType.FIT_CENTER

        headView.addView(headBackgroundIv)
    }

    private fun addBottomMark(headView: FrameLayout) {
        if (!needHeadBottom) return

        val lp1 = LayoutParams(LayoutParams.MATCH_PARENT, headBottomMarkHeight)
        lp1.gravity = Gravity.BOTTOM
        val frameLayout = MyFrameLayout(context)
        frameLayout.layoutParams = lp1
        frameLayout.setBackgroundColor(Color.TRANSPARENT)

        addCircleIv(frameLayout)

        headView.addView(frameLayout)
    }

    private fun addCircleIv(frameLayout: FrameLayout) {
        val lp2 = LayoutParams(headBottomIvSize, headBottomIvSize)
        if (headBottomMarkSlope > 0f) {
            lp2.marginStart = 100
            lp2.gravity = Gravity.CENTER_VERTICAL.or(Gravity.START)
        } else {
            lp2.marginEnd = 100
            lp2.gravity = Gravity.CENTER_VERTICAL.or(Gravity.END)
        }

        val cardView = CardView(context)
        with(cardView) {
            layoutParams = lp2
            radius = headBottomIvSize.toFloat() / 2
            cardElevation = headBottomIvElevation.toFloat()
        }

        headBottomIv = ImageView(context)
        cardView.addView(headBottomIv)

        frameLayout.addView(cardView)
    }

    fun setCustomPartBGColor(color: Int) {
        setBackgroundColor(color)
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
        changeHeadToDefaultStatus(false)
        super.onLayout(changed, l, t, r, b)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return true

        val deltaY = ev.y - lastY
        lastY = ev.y
        // ( 控件到达顶部时继续向下拉 | 控件的头部处在变化中 ) 这两种情况需要将滑动事件拦截
        if (ev.action == MotionEvent.ACTION_MOVE && (scrollY == 0 && deltaY > 0 || isHeadChanged)) {
            changeHeadStatus(deltaY)
            return true
        }

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
        if (curTranslationY > -headHideHeight / 3) {
            val scale = curTranslationY * 2 / headHideHeight + 1
            headBackgroundIv.scaleX = 1 + scale / 4
            headBackgroundIv.scaleY = 1 + scale / 4
        }
        isHeadChanged = true
    }

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
        valueAnimator.interpolator = OvershootInterpolator()
        valueAnimator.duration = 500
        valueAnimator.addUpdateListener {
            val fraction = it.animatedValue as Float
            mainContainer.translationY =
                curTranslationY + (-headHideHeight - curTranslationY) * fraction
            val scale = originalScale + (1f - originalScale) * fraction
            headBackgroundIv.scaleX = scale
            headBackgroundIv.scaleY = scale
        }
        valueAnimator.addListener(object : BaseAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                curTranslationY = -headHideHeight.toFloat()
            }
        })
        valueAnimator.start()
    }

    private inner class MyFrameLayout(context: Context) : FrameLayout(context) {
        val paint: Paint = Paint()

        init {
            with(paint) {
                color = headBottomMarkColor
                style = Paint.Style.FILL_AND_STROKE
                isAntiAlias = true
            }
        }

        override fun onDraw(canvas: Canvas?) {
            val leftStartY = getLeftStartY()
            val rightStartY = getRightStartY()
            val path = getPath(leftStartY, rightStartY)

            canvas?.drawPath(path, paint)
        }

        private fun getLeftStartY(): Float {
            return if (headBottomMarkSlope <= 0) {
                0f
            } else {
                headBottomMarkSlope * height
            }
        }

        private fun getRightStartY(): Float {
            return if (headBottomMarkSlope <= 0) {
                abs(headBottomMarkSlope) * height
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
}