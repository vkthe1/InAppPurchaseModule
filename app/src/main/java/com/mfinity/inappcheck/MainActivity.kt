package com.mfinity.inappcheck

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val context: Activity by lazy {
        this
    }
    val billingManager: BillingManager by lazy {
        (application as InAppApplication).billingManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (billingManager.isPurchased("android.test.purchased")) {
            btnPurchase.text = "Already Purchased"
            btnPurchase.isEnabled = false
            btnPurchase.alpha = .5f
        } else {
            btnPurchase.text = "Purchase Now"

        }
        btnPurchase.setOnClickListener {
            billingManager.callpurchase(context)
        }
    }

}
