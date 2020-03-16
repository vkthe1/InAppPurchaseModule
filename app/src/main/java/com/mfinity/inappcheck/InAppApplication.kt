package com.mfinity.inappcheck

import android.app.Application

class InAppApplication : Application() {

    val billingManager: BillingManager by lazy {
        val products=ArrayList<String>();
        products.add("android.test.purchased")
        BillingManager(this, products)
    }

    override fun onCreate() {
        super.onCreate()
    }
}