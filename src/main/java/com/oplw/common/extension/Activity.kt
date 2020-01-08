package com.oplw.common.extension

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.oplw.common.GhostFragment
import java.io.Serializable

/**
 *
 *   @author opLW
 *   @date  2019/7/11
 */
fun Activity.isNetConnected(): Boolean {
    val manager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val netWokInfo = manager.activeNetworkInfo
    return if (netWokInfo == null) {
        showToastInCenter("网络开小差")
        false
    } else {
        true
    }
}

fun Activity.hideKeyBroad() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
}

fun Activity.showToastInBottom(prompt: String) {
    with(Toast.makeText(this, prompt, Toast.LENGTH_SHORT)) {
        setGravity(Gravity.BOTTOM, 0, 0)
        show()
    }
}

fun Activity.showToastInCenter(prompt: String) {
    with(Toast.makeText(this, prompt, Toast.LENGTH_SHORT)) {
        setGravity(Gravity.CENTER, 0, 0)
        show()
    }
}

fun Activity.showDialog(
    content: String,
    title: String = "注意",
    positiveBtnMsg: String = "确定",
    negativeBtnMsg: String = "取消",
    positiveAction: (() -> Unit)? = null,
    negativeAction: (() -> Unit)? = null
) {
    AlertDialog.Builder(this)
        .setMessage(content)
        .setTitle(title)
        .setPositiveButton(positiveBtnMsg) { dialog, _ ->
            if (positiveAction != null) {
                positiveAction()
            }
            dialog.dismiss()
        }
        .setNegativeButton(negativeBtnMsg) { dialog, _ ->
            if (negativeAction != null) {
                negativeAction()
            }
            dialog.dismiss()
        }
        .show()
}

fun Activity.startActivity(target: Class<out Activity>, vararg params: Pair<String, Any>) {
    val intent = Intent(this, target)
    if (params.isNotEmpty()) {
        intent.putExtras(*params)
    }
    startActivity(intent)
}

fun Activity.startActivityForResult(
    fm: FragmentManager, target: Class<out Activity>,
    vararg params: Pair<String, Any>, callback: (result: Intent?) -> Unit
) {
    val intent = Intent(this, target).putExtras(*params)
    val ghostFragment = GhostFragment()
    ghostFragment.init(intent) {
        callback(intent)
        fm.beginTransaction().remove(ghostFragment).commitAllowingStateLoss()
    }
    fm.beginTransaction().add(ghostFragment, GhostFragment::class.java.simpleName)
        .commitAllowingStateLoss()
}

fun Intent.putExtras(vararg params: Pair<String, Any>): Intent {
    for ((key, value) in params) {
        when (value) {
            is Byte -> putExtra(key, value)
            is Short -> putExtra(key, value)
            is Int -> putExtra(key, value)
            is Long -> putExtra(key, value)
            is Float -> putExtra(key, value)
            is Double -> putExtra(key, value)
            is Char -> putExtra(key, value)
            is CharSequence -> putExtra(key, value)
            is ByteArray -> putExtra(key, value)
            is ShortArray -> putExtra(key, value)
            is IntArray -> putExtra(key, value)
            is LongArray -> putExtra(key, value)
            is FloatArray -> putExtra(key, value)
            is DoubleArray -> putExtra(key, value)
            is CharArray -> putExtra(key, value)
            is Bundle -> putExtra(key, value)
            is Serializable -> putExtra(key, value)
            is Parcelable -> putExtra(key, value)
        }
    }
    return this
}
