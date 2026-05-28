package com.vk.inappcheck

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.Purchase
import com.google.gson.Gson
import com.vk.billing.BillingWrapper
import com.vk.billing.ProductInApp
import com.vk.inappcheck.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ProductInApp {

    private val context: Activity by lazy {
        this
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val billingWrapper: BillingWrapper by lazy {
        (application as InAppApplication).billingWrapper!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        mBinding.btnPurchaseSub.setOnClickListener {
            billingWrapper.getProductDetails().forEach { productDetails ->
                productDetails.subscriptionOfferDetails?.forEach {
                    Log.e("VK", it.pricingPhases.pricingPhaseList[0].formattedPrice)
                }
                Log.e("getProductDetails", "getProductDetails: \n${Gson().toJson(productDetails)}")
            }
        }

        mBinding.btnPurchase.setOnClickListener {
            scope.launch {
                if (billingWrapper.isPurchased(
                        Constant.INAPP_PRODUCT,
                    )
                ) {
                    billingWrapper.consumeProduct(Constant.INAPP_PRODUCT)
                } else
                    billingWrapper.callPurchase(
                        context,
                        (application as InAppApplication).inAppLists[0],
                        this@MainActivity,
                        true
                    )
            }

        }
//        billingWrapper.consumeProduct(Constant.CREDIT10)
        Handler(Looper.getMainLooper()).postDelayed({
            scope.launch {
//                if (billingWrapper.isPurchased(
//                        (application as InAppApplication).subscriptionList[0],
//                        BillingClient.ProductType.SUBS
//                    )
//                ) {
                billingWrapper.getProductDetails().forEach {
                    Log.e("Vk", "Purchase Product ${it.oneTimePurchaseOfferDetails?.formattedPrice}")
                }
//                    billingWrapper.getPurchaseList().forEach {
//                        Log.e("Vk", "Purchase Product ${it.products[0]}")
//                    }
//                    runOnUiThread {
//                        mBinding.btnPurchaseSub.text = "Sub Already Purchased"
//                        mBinding.btnPurchaseSub.isEnabled = false
//                        mBinding.btnPurchaseSub.alpha = .5f
//                    }
//                } else {
//                    runOnUiThread {
//                        mBinding.btnPurchaseSub.text = "Purchase Sub"
//                    }
//                }
            }
        }, 2000)
        /* if (billingWrapper.isPurchased(
                 (application as InAppApplication).inAppLists[0],
                 BillingClient.ProductType.INAPP
             )
         ) {
             btnPurchase.text = "InApp Already Purchased"
             btnPurchase.isEnabled = false
             btnPurchase.alpha = .5f
         } else {
             btnPurchase.text = "Purchase InApp"
         }*/
//        }, 500)

    }

    override fun productList(purchase: List<Purchase>?) {
        for (item: Purchase in purchase!!) {
            Log.e("Vk ", item.products[0])
        }
    }

    @SuppressLint("SetTextI18n")
    override fun isPurchased(product: String) {

//        if (Product == Constant.CREDIT10) {
//            billingWrapper.consumeProduct(Constant.CREDIT10)
//        } else


        if (product == (application as InAppApplication).subscriptionList[0]) {
            mBinding.btnPurchaseSub.text = "Sub Already Purchased"
            mBinding.btnPurchaseSub.isEnabled = false
            mBinding.btnPurchaseSub.alpha = .5f
        } /*else if (product == (application as InAppApplication).inAppLists[0]) {
            btnPurchase.text = "InApp Already Purchased"
            btnPurchase.isEnabled = false
            btnPurchase.alpha = .5f
        }*/
    }

    override fun cancelByUser() {
        Log.e("Vk ", "cancelByUser")
    }

    override fun onPurchaseError() {
        Log.e("Vk ", "onPurchaseError")
    }

    override fun featureNotSupported() {
        Log.e("Vk ", "featureNotSupported")
    }

    override fun serviceTimeOut() {
        Log.e("Vk ", "serviceTimeOut")
    }

    override fun itemAlreadyOwned() {
        Log.e("Vk ", "itemAlreadyOwned: ")
    }

    override fun itemNotOwned() {
        Log.e("PSB ", "itemNotOwned")
    }

    override fun itemNotAvailable() {
        Log.e("PSB ", "itemNotAvailable")
    }

    override fun serviceDisconnected() {
        Log.e("Vk ", "serviceDisconnected")
    }

    override fun onNetworkError() {
        Toast.makeText(this, "Network not available.", Toast.LENGTH_SHORT).show()
        Log.e("PSB ", "onNetworkError")
    }

}
