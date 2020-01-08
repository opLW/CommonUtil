package com.oplw.common.extension

import android.app.Activity
import androidx.fragment.app.Fragment

/**
 *
 *   @author opLW
 *   @date  2019/11/9
 */
fun String.isPhoneNumber(): Boolean{
    val regex = Regex("""^1[3|4|5|8][0-9]\d{8}${'$'}""")
    return this.matches(regex)
}

fun String.isEmail(): Boolean {
    val regex = Regex("""[\w\.\-]+@([\w\-]+\.)+[\w\-]+""")
    return this.matches(regex)
}

fun String.isEmptyToNotify(activity: Activity, remindMsg: String): Boolean {
    return if (this.isEmpty()) {
        activity.showToastInCenter(remindMsg)
        true
    } else {
        false
    }
}

fun String.isEmptyToNotify(fragment: Fragment, remindMsg: String): Boolean {
    return if (this.isEmpty()) {
        fragment.requireActivity().showToastInCenter(remindMsg)
        true
    } else {
        false
    }
}