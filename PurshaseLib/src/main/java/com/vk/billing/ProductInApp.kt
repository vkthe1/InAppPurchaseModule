package com.vk.billing

import com.android.billingclient.api.Purchase

interface ProductInApp {

    fun productList(purchase: List<Purchase>?)

    fun isPurchased(product: String)

    fun cancelByUser()

    fun onPurchaseError()

    fun onNetworkError()

    fun featureNotSupported()

    fun serviceTimeOut()

    fun itemAlreadyOwned()

    fun itemNotOwned()

    fun itemNotAvailable()

    fun serviceDisconnected()
}