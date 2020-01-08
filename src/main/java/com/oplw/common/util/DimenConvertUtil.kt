package com.oplw.common.util

import android.content.Context

/**
 *
 *   @author opLW
 *   @date  2019/12/20
 */
fun dp2px(context: Context, dpValue: Float): Int{
    val d = context.resources.displayMetrics.density
    return Math.round(dpValue * d)
}

fun sp2px(context: Context, spValue: Float): Float{
    val d = context.resources.displayMetrics.scaledDensity
    return Math.round(spValue * d).toFloat()
}