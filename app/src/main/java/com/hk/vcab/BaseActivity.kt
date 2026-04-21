package com.hk.vcab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hk.vcab.R

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // apply fade-in when starting
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun finish() {
        super.finish()
        // apply fade-out when finishing
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
