package com.oplw.common.customview.layout

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 *
 *   @author opLW
 *   @date  2019/7/12
 */
class StatusLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attributeSet, defStyleAttr) {

    private lateinit var normalView: View
    private var curShowingViewId = -1

    fun getNormalView(): View {
        return normalView
    }

    fun showNormalView() {
        if (childCount == 0) {
            throw Exception("The normalView has not been added")
        }
        if (curShowingViewId == normalView.labelFor) return

        removeViewAt(1)
        normalView.visibility = View.VISIBLE
        curShowingViewId = normalView.labelFor
    }

    fun addNormalView(resId: Int): View {
        val view = LayoutInflater.from(context).inflate(resId, this, false)
        addNormalView(view)
        return view
    }

    fun addNormalView(view: View?) {
        if (childCount >= 1) {
            throw Exception("StatusLayout can only keep one normalView, user showAbnormalView() to add other view")
        }
        addView(view)
    }

    fun addAbnormalView(resId: Int): View {
        val view = LayoutInflater.from(context).inflate(resId, this, false)
        addAbnormalView(view)
        return view
    }

    fun addAbnormalView(view: View?) {
        if (childCount == 0) {
            throw Exception("The normalView has not been added")
        }
        addView(view)
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (childCount == 0 && child != null) {
            addNormalViewInner(child)
        } else if (child != null){
            addAbnormalViewInner(child)
        }
        super.addView(child, index, params)
    }

    private fun addNormalViewInner(child: View) {
        normalView = child

        val labelId = View.generateViewId()
        normalView.labelFor = labelId
        curShowingViewId = normalView.labelFor
    }

    private fun addAbnormalViewInner(child: View) {
        if (curShowingViewId == child.labelFor) return

        if (curShowingViewId == normalView.labelFor) {
            normalView.visibility = View.INVISIBLE
        } else {
            removeViewAt(1)
        }

        val labelId = View.generateViewId()
        child.labelFor = labelId
        curShowingViewId = child.labelFor
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
