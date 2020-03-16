package com.mfinity.inappcheck

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    context: Context,
    val skuList: ArrayList<String>
) : PurchasesUpdatedListener {

    companion object {
        const val TAG = "VK"
    }

    lateinit var listSkuDetails: List<SkuDetails>
    val billingClient =
        BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()

    init {
        startConnection()
    }

    private fun startConnection() {
        Log.e(TAG, "startConnection")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "startConnection onBillingSetupFinished with responseCode OK")
                    // The BillingClient is ready. You can query purchases here.
                    CoroutineScope(Dispatchers.IO).launch {
                        getPurchasedList()
                        querySkuDetails()
                    }

                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.e(TAG, "startConnection onBillingServiceDisconnected OK")
                startConnection()
            }
        })
    }


    fun callpurchase(activity: Activity) {
        if (::listSkuDetails.isInitialized && listSkuDetails.isNotEmpty() && billingClient.isReady) {
            val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(listSkuDetails[0])
                .build()
            val responseCode = billingClient.launchBillingFlow(
                activity,
                flowParams
            )
            Log.e(TAG, "Response Code : $responseCode")
        }
    }

    fun isPurchased(sku: String): Boolean {
        var isPurchase = false
        if (billingClient.isReady) {
            val result = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (item: Purchase in result.purchasesList) {
                    if (sku == item.sku && item.purchaseState == Purchase.PurchaseState.PURCHASED)
                        isPurchase = true
                }
            }
        }
        return isPurchase
    }

    fun consumeProduct(sku: String): Boolean {
        var isConsumed = false
        if (billingClient.isReady) {
            val result = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (item: Purchase in result.purchasesList) {
                    if (sku == item.sku && item.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        val consumeParams =
                            ConsumeParams.newBuilder().setPurchaseToken(item.purchaseToken).build()
                        CoroutineScope(Dispatchers.IO).launch {
                            billingClient.consumePurchase(consumeParams)
                            isConsumed = true
                        }
                    }

                }
            }
        }
        return isConsumed
    }


    private suspend fun getPurchasedList() {
        withContext(Dispatchers.IO) {
            val result = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (item: Purchase in result.purchasesList) {
                    Log.e(
                        TAG,
                        "${item.sku} isPurchased : ${item.purchaseState == Purchase.PurchaseState.PURCHASED}"
                    )
                }
            }

        }
    }

    private suspend fun querySkuDetails() {
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient.querySkuDetailsAsync(
                params.build()
            ) { result, skuDetailsList ->
                result?.let {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                        Log.e(TAG, "querySkuDetails : ${skuDetailsList.size}")
                        listSkuDetails = skuDetailsList
                    }
                }
            }
        }
        // Process the result.
    }

    suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user.
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                val ackPurchaseResult = withContext(Dispatchers.IO) {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
                }
            }
        }
    }


    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Log.e(TAG, "onPurchasesUpdated : User responseCode Purchase OK")
            for (purchase in purchases) {
                CoroutineScope(Dispatchers.IO).launch {
                    handlePurchase(purchase)
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.e(TAG, "onPurchasesUpdated : User Canceled Purchase")
        }
    }

}