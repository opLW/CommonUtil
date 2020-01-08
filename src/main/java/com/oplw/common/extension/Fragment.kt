package com.oplw.common.extension

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 *
 *   @author opLW
 *   @date  2019/12/20
 */
fun Fragment.isNetConnected(): Boolean {
    val manager =
        requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val netWokInfo = manager.activeNetworkInfo
    return if (netWokInfo == null) {
        showToastInCenter("网络开小差")
        false
    } else {
        true
    }
}

fun Fragment.hideKeyBroad() {
    val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
}

fun Fragment.showToastInBottom(prompt: String) {
    with(Toast.makeText(context!!, prompt, Toast.LENGTH_SHORT)) {
        setGravity(Gravity.BOTTOM, 0, -150)
        show()
    }
}

fun Fragment.showToastInCenter(prompt: String) {
    with(Toast.makeText(context!!, prompt, Toast.LENGTH_SHORT)) {
        setGravity(Gravity.CENTER, 0, 0)
        show()
    }
}

fun Fragment.showDialog(
    content: String,
    title: String = "注意",
    positiveBtnMsg: String = "确定",
    negativeBtnMsg: String = "取消",
    positiveAction: (() -> Unit)? = null,
    negativeAction: (() -> Unit)? = null
) {
    AlertDialog.Builder(context!!)
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

fun Fragment.startActivity(target: Class<out Activity>, vararg params: Pair<String, Any>) {
    val intent = Intent(requireActivity(), target)
    if (params.isNotEmpty()) {
        intent.putExtras(*params)
    }
    startActivity(intent)
}