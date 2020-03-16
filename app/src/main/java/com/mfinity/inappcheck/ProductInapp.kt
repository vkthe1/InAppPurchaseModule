package com.mfinity.inappcheck

import com.android.billingclient.api.PurchasesUpdatedListener

abstract class ProductInapp: PurchasesUpdatedListener {

    abstract fun getProductList():ArrayList<String>
}