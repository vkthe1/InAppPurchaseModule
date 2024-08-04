
# Google InApp Purchase

A Library to manage the InApp and Subscription purchase with ease.


## Init Module

Init Blling Wrapper from Application class and add your Product Id's of InApp and Subscription in respective List. This method will intialize and fetch all details of product Id's you have provided.


```
val subscriptionList: ArrayList<String> = ArrayList()
val inAppLists: ArrayList<String> = ArrayList()

inAppLists.add(Constant.INAPP_PRODUCT)
subscriptionList.add(Constant.SUB_PRODUCT)

val billingWrapper = BillingWrapper(this, subscriptionList, inAppLists)
```
#

With `getProductDetails()` you can manage to show pricing of product Ids. This method will provide a list of ProductDetail. 

```
billingWrapper.getProductDetails().forEach {

    //To access Price of InApp Product
    Log.e("Vk", "Product Details ${it.oneTimePurchaseOfferDetails?.formattedPrice}")

    //To access Price of Subscription Product
    it.subscriptionOfferDetails?.forEach { item ->
        Log.e("VK", item.pricingPhases.pricingPhaseList[0].formattedPrice)
    }
}

```
#

With `getPurchaseList()` you can will get all the active purchases. 

```
billingWrapper.getPurchaseList().forEach {
    Log.e("Vk", "Purchase Product ${it.products[0]}")
}

```

#

```
/* fun callPurchase(activity : Activity,product : String,purchaseListener : ProductInApp,
isConsumable : Boolean = false)

**isConsumable** is used to auto consume InApp purchase when required
*/

billingWrapper.callPurchase(context,Constant.INAPP_PRODUCT,  purchaseListener,true)
```
