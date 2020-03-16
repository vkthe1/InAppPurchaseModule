package com.mfinity.inappcheck

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class Splash:AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnPurchase.visibility= View.GONE
        Handler().postDelayed({
            startActivity(Intent(this@Splash,MainActivity::class.java))
            finish()
        },2000)


    }
}