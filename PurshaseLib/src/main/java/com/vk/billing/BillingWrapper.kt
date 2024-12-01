package com.vk.billing

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryPurchasesAsync
import com.vk.purshaselib.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingWrapper(
    private val context: Context,
    val subscriptionList: ArrayList<String>,
    val inAppList: ArrayList<String>
) : PurchasesUpdatedListener {

    companion object {
        const val TAG = "vkthe1"
    }

    private var isConsumableProduct: Boolean = false
    private var purchaseProduct: String = String()
    private var inAppListener: ProductInApp? = null
    private var listProductDetails = ArrayList<ProductDetails>()
    private var purchaseList = ArrayList<Purchase>()
    private var pendingPurchaseList = ArrayList<Purchase>()
    private val billingClient =
        BillingClient.newBuilder(context).setListener(this).enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        ).build()
    private var isShowCustomNetworkDialog = false

    fun setShowCustomNetworkDialog(isShowCustomNetworkDialog: Boolean) {
        this.isShowCustomNetworkDialog = isShowCustomNetworkDialog
    }

    fun isBillingReady(): Boolean {
        return billingClient.isReady
    }

    init {
        startConnection()
    }

    fun getPurchaseList(): ArrayList<Purchase> {
        return purchaseList
    }

    fun getPendingPurchasedList(): ArrayList<Purchase> {
        return pendingPurchaseList
    }

    fun getProductDetails(): ArrayList<ProductDetails> {
        return listProductDetails
    }

    private fun startConnection(onFetchSuccess: (() -> Unit?)? = null) {
        purchaseList.clear()
        pendingPurchaseList.clear()
        listProductDetails.clear()
        Log.e(TAG, "startConnection")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "startConnection onBillingSetupFinished with responseCode OK")
                    // The BillingClient is ready. You can query purchases here.
                    CoroutineScope(Dispatchers.IO).launch {
                        if (subscriptionList.isNotEmpty()) {
                            val subList = async {
                                queryProductDetails(
                                    subscriptionList,
                                    BillingClient.ProductType.SUBS
                                )
                            }
                            subList.start()
                        }
                        if (inAppList.isNotEmpty()) {
                            val inAppList =
                                async {
                                    queryProductDetails(
                                        inAppList,
                                        BillingClient.ProductType.INAPP
                                    )
                                }
                            inAppList.start()
                        }

                        getPurchasedList(BillingClient.ProductType.SUBS)?.let {
                            it.forEach { purchase ->
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                    purchaseList.add(purchase)
                                    handlePurchase(purchase)
                                } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                                    pendingPurchaseList.add(purchase)
                                }
                            }
                        }
                        getPurchasedList(BillingClient.ProductType.INAPP)?.let {
                            it.forEach { purchase ->
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                    purchaseList.add(purchase)
                                    handlePurchase(purchase)
                                } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                                    pendingPurchaseList.add(purchase)
                                }
                            }
                        }

                        onFetchSuccess?.let { it() }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.e(TAG, "startConnection onBillingServiceDisconnected OK")
//                startConnection()
            }
        })
    }



    fun callPurchase(
        activity: Activity,
        product: String,
        inAppListener: ProductInApp,
        isConsumable: Boolean = false
    ) {
        purchaseProduct = product
        isConsumableProduct = isConsumable
        this.inAppListener = inAppListener
        if (listProductDetails.isNotEmpty() && isBillingReady()) {
            val productDetail = listProductDetails.find { it.productId == product }
            productDetail?.let {
                val offerToken = it.subscriptionOfferDetails?.let { subOffer ->
                    subOffer[0].offerToken
                } ?: ""
                val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
                with(builder) {
                    setProductDetails(productDetail)
                    if (offerToken.isNotEmpty())
                        setOfferToken(offerToken)
                }
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(arrayListOf(builder.build()))
                    .build()
                val responseCode = billingClient.launchBillingFlow(
                    activity,
                    flowParams
                )
                Log.e(TAG, "Response Code : $responseCode")
            }
        } else {
            if (isNetworkAvailable(activity)) {
                if (listProductDetails.isEmpty()) startConnection {
                    callPurchase(activity, product, inAppListener, isConsumable)
                }
            } else {
                inAppListener.onNetworkError()
                if (!isShowCustomNetworkDialog) {
                    AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.txt_no_internet_connection))
                        .setMessage(activity.getString(R.string.txt_no_internet_connection_message))
                        .setPositiveButton(activity.getString(R.string.txt_positive_btn)) { dialog, which ->
                            dialog.dismiss()
                        }.show()
                }
            }
        }
    }

    suspend fun isPurchased(
        product: String,
        productType: String = BillingClient.ProductType.INAPP
    ) =
        withContext(Dispatchers.IO) {
            var isPurchase = false
            val purchaseList =
                billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(productType).build()
                ).purchasesList
            for (item: Purchase in purchaseList) {
                if (product == item.products[0] && item.purchaseState == Purchase.PurchaseState.PURCHASED)
                    isPurchase = true
            }
            return@withContext isPurchase
        }

    suspend fun consumeProduct(
        product: String,
        productType: String = BillingClient.ProductType.INAPP
    ): Boolean {
        var isConsumed = false
        if (isBillingReady()) {
            val result = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(productType)
                    .build()
            ).purchasesList
            for (item: Purchase in result) {
                if (product == item.products[0] && item.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    val consumeParams =
                        ConsumeParams.newBuilder().setPurchaseToken(item.purchaseToken).build()
                    CoroutineScope(Dispatchers.IO).launch {
                        billingClient.consumeAsync(consumeParams) { billingResult, outToken ->
                            Log.e(
                                "Vk",
                                "${billingResult.responseCode == BillingClient.BillingResponseCode.OK} : $outToken"
                            )
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                Log.e("Vk", "Purchases Consume")
                            } else
                                Log.e(
                                    "VK ${billingResult.responseCode}",
                                    billingResult.debugMessage
                                )
                        }
                        isConsumed = true
                    }
                }
            }
        }
        return isConsumed
    }

    private suspend fun getPurchasedList(productType: String = BillingClient.ProductType.INAPP) =
        withContext(Dispatchers.IO) {
            if (isBillingReady()) {
                return@withContext billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(productType)
                        .build()
                ).purchasesList
            } else
                return@withContext null
        }

    private suspend fun queryProductDetails(
        arrayList: ArrayList<String>,
        productType: String = BillingClient.ProductType.INAPP
    ) = withContext(Dispatchers.IO) {
        val productList = ArrayList<QueryProductDetailsParams.Product>()
        for (item in arrayList) {
            productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(item)
                    .setProductType(productType)
                    .build()
            )
        }
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
        billingClient.queryProductDetailsAsync(
            queryProductDetailsParams
        ) { billingResult, productDetailsList ->
            billingResult.let {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "queryProductDetails : ${productDetailsList.size}")
                    if (productDetailsList.isNotEmpty()) {
                        listProductDetails.addAll(productDetailsList)
                    }
                }
            }
        }
        return@withContext listProductDetails
    }

    // Process the result.
    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user.
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                withContext(Dispatchers.IO) {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
                }
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                else -> false
            }
        } else {
            return connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when {
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                Log.e(TAG, "onPurchasesUpdated : User responseCode Purchase OK")

                purchaseList.clear()
                pendingPurchaseList.clear()
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                        purchaseList.add(purchase)
                    else (purchase.purchaseState == Purchase.PurchaseState.PENDING)
                        pendingPurchaseList.add(purchase)

                }
                inAppListener?.productList(purchaseList)
                for (purchase in purchases) {
                    if (purchase.products[0] == purchaseProduct) {
                        Log.e("Vk", " Cosumable : $isConsumableProduct")
                        if (isConsumableProduct) {
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(1000)
                                consumeProduct(purchase.products[0])
                            }
                        }
                        inAppListener?.isPurchased(purchase.products[0])
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        handlePurchase(purchase)
                    }
                }
            }


            billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Handle an error caused by a user cancelling the purchase flow.
                Log.e(TAG, "onPurchasesUpdated : User Purchase Already Owned")
                inAppListener?.itemAlreadyOwned()

            }

            billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                // Handle an error caused by a user cancelling the purchase flow.
                Log.e(TAG, "onPurchasesUpdated : User Canceled Purchase")
                inAppListener?.cancelByUser()

            }

            billingResult.responseCode == BillingClient.BillingResponseCode.ERROR -> {
                // Handle error
                Log.e(TAG, "onPurchasesUpdated : Error")
                inAppListener?.onPurchaseError()
            }

            billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                // Handle error
                Log.e(TAG, "onPurchasesUpdated : FEATURE_NOT_SUPPORTED")
                inAppListener?.featureNotSupported()
            }

            billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                // Handle error
                Log.e(TAG, "onPurchasesUpdated : SERVICE_UNAVAILABLE")
                inAppListener?.serviceTimeOut()
            }

            billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                // Handle error
                Log.e(TAG, "onPurchasesUpdated : BILLING_UNAVAILABLE")
                inAppListener?.onPurchaseError()
            }

            billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                // Handle error
                Log.e(TAG, "onPurchasesUpdated : SERVICE_DISCONNECTED")
                inAppListener?.serviceDisconnected()
            }

            billingResult.responseCode == BillingClient.BillingResponseCode.NETWORK_ERROR -> {
                // Handle error
                Log.e(TAG, "onPurchasesUpdated : NETWORK_ERROR")
                inAppListener?.onNetworkError()
            }
        }
    }


    /**
     * Returns true if the grace period option should be shown.
     */
//    fun isGracePeriod(subscription: SubscriptionStatus?) =
//        subscription != null &&
//                subscription.isEntitlementActive &&
//                subscription.isGracePeriod &&
//                !subscription.subAlreadyOwned


}