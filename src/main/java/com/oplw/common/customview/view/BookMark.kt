package com.oplw.common.customview.view

import android.content.Context
import android.graphics.*
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView

// 需前往value/attrs.xml文件中获取相应的styleable
import com.oplw.common.R

/**
 *
 *   @author opLW
 *   @date  2019/11/6
 */
class BookMark @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextView(context, attr, defStyleAttr) {

    private var paint = Paint()
    private var isSinking = false
    private var blurMask: BlurMaskFilter

    /**
     * 左上角要裁剪掉部分的水平长度
     */
    private val leftClipX: Int
    /**
     * 右上角要裁剪掉部分的水平长度
     */
    private val rightClipX: Int
    private val noClipCornerRadius: Int
    private val clipCornerRadius: Int
    private val backgroundColor: Int
    private val bShadowDx: Float
    private val bShadowDy: Float
    private val bShadowRadius: Float
    private val bShadowColor: Int

    /*
    记录控件初始化时各个padding的大小，
    避免由于系统多次measure导致多次调用setPaddingToDrawBG，影响初始padding的大小。
     */
    private val paddingL: Int
    private val paddingT: Int
    private val paddingR: Int
    private val paddingB: Int

    /**
     * 左上角最终裁剪的大小，受控件整体的宽度限制产生的结果
     */
    private var clipSizeL = -1f
    private var clipSizeR = -1f

    /**
     * 四个方向上阴影的大小
     */
    private var shadowSizeL = -1f
    private var shadowSizeR = -1f
    private var shadowSizeT = -1f
    private var shadowSizeB = -1f
    private val clipBorderL: Float
        get() {
            return shadowSizeL
        }
    private val clipBorderR: Float
        get() {
            return width - shadowSizeR
        }

    /**
     * 内容物的左边界，边界右侧为不受裁剪影响部分，即安全区域。
     * 同理的还有contentBorderT、contentBorderR、contentBorderB
     */
    private val contentBorderL: Float
        get() {
            return shadowSizeL + clipSizeL
        }
    private val contentBorderT: Float
        get() {
            return shadowSizeT
        }
    private val contentBorderR: Float
        get() {
            return width - shadowSizeR - clipSizeR
        }
    private val contentBorderB: Float
        get() {
            return height - shadowSizeB
        }

    init {
        // 没有开启参数没办法完全显示阴影
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val typeArray = context.obtainStyledAttributes(attr, R.styleable.BookMark)
        with(typeArray) {
            leftClipX = getDimensionPixelSize(R.styleable.BookMark_leftClipX, 0)
            rightClipX = getDimensionPixelSize(R.styleable.BookMark_rightClipX, 0)
            noClipCornerRadius = getDimensionPixelSize(R.styleable.BookMark_noClipCornerRadius, 10)
            clipCornerRadius = getDimensionPixelSize(R.styleable.BookMark_clipCornerRadius, 10)
            backgroundColor = getColor(R.styleable.BookMark_backgroundColor, Color.WHITE)
            bShadowDx = getFloat(R.styleable.BookMark_bShadowDx, 0f)
            bShadowDy = getFloat(R.styleable.BookMark_bShadowDy, 0f)
            bShadowRadius = getFloat(R.styleable.BookMark_bShadowRadius, 0f)
            bShadowColor = getColor(R.styleable.BookMark_bShadowColor, Color.GRAY)
            isSinking = getBoolean(R.styleable.BookMark_isSinking, false)
            val textBlurRadius = getFloat(R.styleable.BookMark_textBlurRadius, 5f)
            blurMask = BlurMaskFilter(textBlurRadius, BlurMaskFilter.Blur.NORMAL)
            recycle()
        }

        paddingL = paddingLeft
        paddingT = paddingTop
        paddingR = paddingRight
        paddingB = paddingBottom
    }

    override fun performClick(): Boolean {
        this.isSinking = !this.isSinking
        invalidate()
        return super.performClick()
    }

    fun setSinking(isSinking: Boolean) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw Exception("Can`t update UI in not UI thread")
        }
        this.isSinking = isSinking
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setPaddingToDrawBG()

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun setPaddingToDrawBG() {
        val left = calculatePaddingLeft().toInt()
        val top = calculatePaddingTop().toInt()
        val right = calculatePaddingRight().toInt()
        val bottom = calculatePaddingBottom().toInt()

        setPadding(left, top, right, bottom)
    }

    private fun calculatePaddingLeft(): Float {
        clipSizeL = Math.min(measuredWidth / 4, Math.max(leftClipX, 0)).toFloat()
        shadowSizeL = Math.max(0f, -bShadowDx) + Math.max(0f, bShadowRadius)
        return clipSizeL + shadowSizeL + paddingL
    }

    private fun calculatePaddingTop(): Float {
        shadowSizeT = Math.max(0f, -bShadowDy) + Math.max(0f, bShadowRadius)
        return shadowSizeT + paddingT
    }

    private fun calculatePaddingRight(): Float {
        clipSizeR = Math.min(measuredWidth / 4, Math.max(rightClipX, 0)).toFloat()
        shadowSizeR = Math.max(0f, bShadowDx) + Math.max(0f, bShadowRadius)
        return clipSizeR + shadowSizeR + paddingR
    }

    private fun calculatePaddingBottom(): Float {
        shadowSizeB = Math.max(0f, bShadowDy) + Math.max(0f, bShadowRadius)
        return shadowSizeB + paddingB
    }

    override fun onDraw(canvas: Canvas?) {
        drawBackground(canvas)

        if (isSinking) {
            getPaint().maskFilter = blurMask
        } else {
            getPaint().maskFilter = null
        }
        super.onDraw(canvas)
    }

    private fun drawBackground(canvas: Canvas?) {
        with(paint) {
            paint.reset()
            style = Paint.Style.FILL_AND_STROKE
            strokeCap = Paint.Cap.ROUND
            color = backgroundColor
            isAntiAlias = true
            val factor = if (isSinking) 0.5f else 1.0f
            setShadowLayer(bShadowRadius, bShadowDx * factor, bShadowDy * factor, bShadowColor)
        }

        canvas?.drawPath(createPath(), paint)
    }

    private fun createPath(): Path {
        val path = Path()
        createLeftPart(path)
        createRightPart(path)
        return path
    }

    private fun createLeftPart(path: Path) {
        val midWidth = width / 2f
        path.moveTo(midWidth, contentBorderT)

        if (leftClipX > 0) {
            path.arcTo(
                contentBorderL, contentBorderT, contentBorderL + clipCornerRadius * 2,
                contentBorderT + clipCornerRadius * 2, 270f, -50f, false
            )
        } else {
            val noClipCornerDia = getSuitableRadius() * 2
            path.arcTo(
                contentBorderL, contentBorderT, contentBorderL + noClipCornerDia, contentBorderT + noClipCornerDia,
                270f, -90f, false
            )
        }

        path.lineTo(clipBorderL, contentBorderB)
        path.lineTo(midWidth, contentBorderB)
    }

    private fun createRightPart(path: Path) {
        val midWidth = width / 2f
        path.lineTo(midWidth, contentBorderT)

        if (rightClipX > 0) {
            path.arcTo(
                contentBorderR - clipCornerRadius * 2, contentBorderT, contentBorderR,
                contentBorderT + clipCornerRadius * 2, 270f, 50f, false
            )
        } else {
            val noClipCornerD = getSuitableRadius() * 2
            path.arcTo(
                contentBorderR - noClipCornerD, contentBorderT, contentBorderR, contentBorderT + noClipCornerD,
                270f, 90f, false
            )
        }

        path.lineTo(clipBorderR, contentBorderB)
        path.lineTo(midWidth, contentBorderB)
    }

    private fun getSuitableRadius(): Float {
        val contentWidth = contentBorderR - contentBorderL
        val contentHeight = contentBorderB - contentBorderT
        val minSideL = Math.min(contentWidth, contentHeight)
        // 限制圆角的半径
        return Math.min(minSideL / 2, noClipCornerRadius.toFloat())
    }
}
