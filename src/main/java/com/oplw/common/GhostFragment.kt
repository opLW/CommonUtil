package com.oplw.common

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment

/**
 *
 *   @author opLW
 *   @date  2019/11/4
 */
class GhostFragment: Fragment() {
    private companion object{
        var requestCode: Int = 0
            set(value) {
                field = if (requestCode > Integer.MAX_VALUE) 0 else value
            }
    }

    private var intent: Intent? = null
    private lateinit var callback: (Intent?) -> Unit

    fun init(intent: Intent, callback: (Intent?) -> Unit): GhostFragment {
        this.intent = intent
        this.callback = callback
        return this
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCode) {
            if (resultCode == 200 && data != null) {
                callback(data)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        intent?.let{ startActivityForResult(intent, requestCode) }
    }

    override fun onDetach() {
        super.onDetach()
        intent = null
        requestCode++
    }
}