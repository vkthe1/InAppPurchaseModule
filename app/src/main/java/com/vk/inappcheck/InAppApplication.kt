package com.vk.inappcheck

import android.app.Application
import com.vk.billing.BillingWrapper

class InAppApplication : Application() {

    var subscriptionList: ArrayList<String> = ArrayList()
    var inAppLists: ArrayList<String> = ArrayList()

    var billingWrapper: BillingWrapper? = null

    override fun onCreate() {
        super.onCreate()
        inAppLists.add(Constant.INAPP_PRODUCT)
        subscriptionList.add(Constant.SUB_PRODUCT)
        billingWrapper = BillingWrapper(this, subscriptionList, inAppLists)
    }
}