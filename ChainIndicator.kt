package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.sqrt

/**
 * @description
 *
 * onLayout和onMeasure只能通过measureHeight获取高度，onDraw中则可通过height获取
 *
 * @author zhengzhenhui(zhenzhenhui@rd.netease.com)
 * @date 2020/8/3 11:43
 */
class ChainIndicator @JvmOverloads constructor(
    context: Context, attributeSet: AttributeSet? = null, defStyleAttrInt: Int = 0
) : ViewGroup(context, attributeSet, defStyleAttrInt) {

    // 控件当前移动到哪个位置
    private var curPosition: Int = 0

    private var pendantTitles = mutableListOf<String>()
    private val pendantForm: Pendant.PendantForm
    private var pendantShapeType = ShapeType.Round
    // 默认有三个Pendant
    private var defaultPendantCount = 3
    private var pendantPool = mutableListOf<Pendant>()
    // Pendant之间的距离
    private var pendantMargin = 200
    private var curOffsetX = 0f

    private val paint = Paint()
    private var lineWidth = 10f
    private var lineColor = Color.RED

    // 控件中每一条弧线的最高点和最低点的高度差
    private var lineHeightGap = 48
    private var lineRectF = RectF()
    private var radius: Float? = null
        get() {
            if (null == field) {
                val b = measuredWidth - paddingLeft - paddingRight
                field = calculateCircleRadius(lineHeightGap, b)
            }
            return field
        }
    private var line2Bot = 90
    private var botColor = Color.WHITE
    private var botRectF = RectF()

    init {
        with(paint) {
            strokeCap = Paint.Cap.ROUND
            strokeWidth = lineWidth
            color = lineColor
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        // TODO 根据外部数据进行替换
        pendantForm = Pendant.PendantForm(
            10,
            20f,
            Color.BLACK,
            10
        )

        setWillNotDraw(false)
    }

    fun setCurrentItem(position: Int) {
        if (position >= pendantTitles.size) return
        // TODO
    }

    /**
     * 对外提供更新数据的接口
     */
    fun setPendantTitles(titles: List<String>) {
        pendantTitles.clear()
        pendantTitles.addAll(titles)

        // 对已经存在的Pendant进行数据更新
        for (i in 0 until childCount) {
            val pendantContent = this[i]?.pendantContent
            pendantContent?.let {
                if (it.position < titles.size) {
                    // pendant对应的位置有数据则刷新
                    it.text = titles[it.position]
                } else {
                    // pendant对应位置没有数据则回收自己
                    this[i]?.run {
                        removeViewAt(i)
                        this.clearContent()
                        pendantPool.add(this)
                    }
                }
            }
        }
        // 如果当前不足默认个数，则需要判断是否添加
        while (childCount < defaultPendantCount) {
            val content = this[childCount - 1]?.pendantContent
            val lastPosition = content?.position ?: -1
            if (lastPosition + 1 < pendantTitles.size) {
                val pendant = getPendant(position = lastPosition + 1)
                addView(pendant)
            } else {
                break;
            }
        }
    }

    private fun getPendant(position: Int): Pendant {
        val pendant = if (pendantPool.isEmpty()) {
            createPendant()
        } else {
            pendantPool.removeAt(0)
        }

        val text = if (position < pendantTitles.size) pendantTitles[position] else ""
        pendant.setPendantContent(Pendant.PendantContent(text, position))
        return pendant
    }

    private fun createPendant(): Pendant {
        // 扩展Shape类型
        return if (pendantShapeType == ShapeType.Round) {
            RoundPendant(context, pendantForm)
        } else {
            RoundPendant(context, pendantForm)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                widthSize
            }
            else -> {
                0
            }
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                heightSize
            }
            else -> {
                val childHeight = this[0]?.measuredHeight ?: 0
                paddingBottom + line2Bot + lineHeightGap + childHeight
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    }

    private fun layoutPendant(pendant: Pendant?, relativeOffsetX: Float) {
        if (null == pendant) return

        val point = calculateInterPoint(measuredWidth / 2f, relativeOffsetX, lineRectF.top, radius!!)
        val location = pendant.getLocationInChain(point)
        pendant.layout(
            location.left.toInt(),
            location.top.toInt(),
            location.right.toInt(),
            location.bottom.toInt()
        )
    }

    override fun onDraw(canvas: Canvas?) {
        if (null == canvas) return

        // 绘制上部分的弧线
        canvas.drawArc(lineRectF, 0f, -180f, false, paint)
        // 绘制弧形底部
        botRectF.left = lineRectF.left
        botRectF.right = lineRectF.right
        botRectF.top = lineRectF.top + line2Bot
        botRectF.bottom = lineRectF.bottom + line2Bot
        with(paint) {
            reset()
            color = botColor
            strokeWidth = 20f
            style = Paint.Style.FILL_AND_STROKE
        }
        canvas.drawArc(botRectF, 0f, -180f, false, paint)
        // 绘制padding
        with(paint) {
            reset()
            color = Color.WHITE
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
        canvas.drawRect(
            0f,
            (height - paddingBottom).toFloat(),
            width.toFloat(),
            height.toFloat(),
            paint
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 逆向推出目标Arc的Rect
        val left2CircleLeft = radius!! - w / 2
        lineRectF.left = -left2CircleLeft
        lineRectF.right = w + left2CircleLeft
        lineRectF.top = h - lineHeightGap - line2Bot - paddingBottom.toFloat()
        lineRectF.bottom = lineRectF.top + 2 * radius!!
    }

    /**
     * 根据ChainPendant的弧线对应圆形的半径
     *
     * 计算公式参考：https://wapbaike.baidu.com/item/%E5%BC%A7%E9%95%BF%E8%AE%A1%E7%AE%97%E5%85%AC%E5%BC%8F
     */
    private fun calculateCircleRadius(h: Int, b: Int): Float {
        return (b * b + 4 * h * h) / (8f * h)
    }

    /**
     * 根据圆心坐标及交点的横坐标计算交点坐标的纵坐标，为了简化计算将圆心默认为(0，0)
     * @param centerX 圆心的实际X坐标
     * @param targetX 目标点的X坐标，相对于[centerX]而言，左则负，右则正
     */
    private fun calculateInterPoint(
        centerX: Float,
        targetX: Float,
        circleTop: Float,
        radius: Float
    ): Point {
        val y = sqrt((radius * radius - targetX * targetX).toDouble()).toFloat()
        val targetY = circleTop + (radius - y)
        return Point(centerX + targetX, targetY)
    }

    private operator fun ViewGroup.get(index: Int): Pendant? {
        return if (index in 0 until childCount) {
            getChildAt(index) as Pendant
        } else {
            null
        }
    }

    private data class Point(
        val x: Float,
        val y: Float
    )

    // 图案的类型
    enum class ShapeType {
        Round
    }

    private abstract class Pendant(context: Context, private var pendantForm: PendantForm) :
        LinearLayout(context) {
        var pendantContent: PendantContent? = null
            private set

        init {
            orientation = VERTICAL

            val tvLP = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            tvLP.gravity = Gravity.CENTER_HORIZONTAL
            val textView = TextView(context).also {
                it.textSize = pendantForm.textSize
                it.setTextColor(pendantForm.textColor)
                it.typeface = pendantForm.typeFace
                // TODO 限制最多字体数
                it.layoutParams = tvLP
            }

            val midMargin = View(context)
            val midLp = LayoutParams(LayoutParams.MATCH_PARENT, pendantForm.midMargin)
            midMargin.layoutParams = midLp

            val shapeLP = generateShapeLP()
            shapeLP.gravity = Gravity.CENTER_HORIZONTAL
            val shape = createShape()
            shape.layoutParams = shapeLP

            if (Position.ShapeTop == pendantForm.pointPosition) {
                addView(shape)
                addView(midMargin)
                addView(textView)
            } else {
                addView(textView)
                addView(midMargin)
                addView(shape)
            }
        }

        fun setPendantContent(pendantContent: PendantContent) {
            this.pendantContent = pendantContent
            setTitle(pendantContent.text)
            requestLayout()
        }

        fun clearContent() {
            this.pendantContent = null
            setTitle("")
        }

        private fun setTitle(title: String) {
            val textView = if (Position.ShapeTop == pendantForm.pointPosition) {
                getChildAt(2) as TextView
            } else {
                getChildAt(0) as TextView
            }
            textView.text = title
        }

        /**
         * 计算Pendant在父布局中位置
         *
         * @param anchor 父布局的参考点
         * @return 指代位置的Rect
         */
        fun getLocationInChain(anchor: Point): RectF {
            val padding = getShapePadding()
            val rectF = RectF()
            rectF.left = anchor.x - padding.left
            rectF.top = anchor.y - padding.top
            rectF.right = anchor.x + padding.right
            rectF.bottom = anchor.y + padding.bottom
            return rectF
        }

        /**
         * 以Pendant图形部分的中心为参考点，设置到pendant各条边的padding
         */
        private fun getShapePadding(): RectF {
            val padding = RectF()
            padding.left = measuredWidth / 2f
            padding.right = measuredWidth / 2f
            if (Position.ShapeTop == pendantForm.pointPosition) {
                padding.top = getChildAt(0).measuredHeight / 2f
                padding.bottom = measuredHeight - padding.top
            } else {
                padding.bottom = getChildAt(2).measuredHeight / 2f
                padding.top = measuredHeight - padding.bottom
            }
            return padding
        }

        abstract fun createShape(): View

        abstract fun generateShapeLP(): LayoutParams

        // 图案部分位于文字的上方或下方
        enum class Position {
            ShapeTop, ShapeBellow
        }

        // 控件的外形样式
        data class PendantForm(
            val midMargin: Int,
            val textSize: Float,
            val textColor: Int,
            val maxLength: Int,
            val pointPosition: Position = Position.ShapeBellow,
            val typeFace: Typeface = Typeface.DEFAULT
        )

        data class PendantContent(
            var text: String,
            var position: Int
        )
    }

    private class RoundPendant(context: Context, pendantForm: PendantForm) :
        Pendant(context, pendantForm) {
        override fun createShape(): View {
            return RoundShape(
                context,
                Color.RED,
                5f,
                Color.YELLOW,
                20f
            )
        }

        override fun generateShapeLP(): LayoutParams {
            return LayoutParams(
                40, 40
            )
        }
    }

    /**
     * 悬挂点的形状
     */
    abstract class Shape(context: Context) : View(context) {
        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            canvas?.let {
                drawShape(it)
            }
        }

        abstract fun drawShape(canvas: Canvas)
    }

    class RoundShape(
        context: Context,
        private val centerColor: Int,
        private val centerRadius: Float,
        private val borderColor: Int,
        private val borderRadius: Float
    ) :
        Shape(context) {
        private val paint = Paint()

        init {
            paint.isAntiAlias = true
            paint.style = Paint.Style.FILL_AND_STROKE
        }

        override fun drawShape(canvas: Canvas) {
            paint.color = centerColor
            canvas.drawCircle(width / 2f, measuredHeight / 2f, centerRadius, paint)
            paint.color = borderColor
            canvas.drawCircle(width / 2f, measuredHeight / 2f, borderRadius, paint)
        }
    }
}