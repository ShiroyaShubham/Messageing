package messenger.messages.messaging.sms.chat.meet.subscription

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import com.google.common.collect.ImmutableList
import com.google.gson.Gson
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.adapters.SubscriptionPlanAdapter
import messenger.messages.messaging.sms.chat.meet.extensions.getSharedPrefs
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass.Companion.hasNavBar
import messenger.messages.messaging.sms.chat.meet.utils.*


class InAppPurchaseDialogFragment : DialogFragment() {
    var tv_declaration: TextView? = null
    var close_dialog: ImageView? = null
    var progressBar: ProgressBar? = null
    var mView: View? = null
    var rvSubsPlan: RecyclerView? = null
    var imgBackground: ImageView? = null
    private var billingClient: BillingClient? = null
    private lateinit var subscriptionPlanAdapter: SubscriptionPlanAdapter
    private lateinit var itemArrayList: ArrayList<itemDS>

    override fun getTheme(): Int {
        return R.style.FullScreenDialog
    }


    override fun onCreateView(
        layoutInflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?
    ): View? {
        mView = layoutInflater.inflate(R.layout.flagment_in_app_purchase_dialog, viewGroup, false)
        tv_declaration = mView?.findViewById(R.id.tv_declaration)
        close_dialog = mView?.findViewById(R.id.close_dialog)
        progressBar = mView?.findViewById(R.id.progressBar)
        rvSubsPlan = mView?.findViewById(R.id.rvSubscriptionPlan)
        imgBackground = mView?.findViewById(R.id.imgBackground)
        setAppDeclation()

        itemArrayList = arrayListOf()


        setUpRecyclerView()


        if (Utils.isDarkMode(requireContext())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imgBackground?.setImageResource(R.drawable.ic_splash_night)
            } else {
                imgBackground?.setImageResource(R.drawable.ic_splash_night_small)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imgBackground?.setImageResource(R.drawable.ic_splash_bg)
            }else{
                imgBackground?.setImageResource(R.drawable.ic_splash_small_bg)
            }
        }


        close_dialog!!.setOnClickListener { view: View? -> dismiss() }
        if (hasNavBar(requireActivity())) {
            val layoutParams = tv_declaration!!.layoutParams as LinearLayout.LayoutParams
            layoutParams.setMargins(0, 0, 0, 0)
            tv_declaration!!.layoutParams = layoutParams
        }
        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initInAppPurchase()
    }

    private fun setUpRecyclerView() {
        subscriptionPlanAdapter = SubscriptionPlanAdapter { productDetail ->
            onPurchaseClick(productDetail)
        }
        rvSubsPlan?.apply {
            adapter = subscriptionPlanAdapter
        }
    }

    private fun onPurchaseClick(productDetails: ProductDetails) {
//        assert(productDetails.subscriptionOfferDetails != null)
        val productDetailsParamsList = ImmutableList.of(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient!!.launchBillingFlow(requireActivity(), billingFlowParams)

    }

    private fun setAppDeclation() {
        tv_declaration!!.isClickable = true
        tv_declaration!!.movementMethod = LinkMovementMethod.getInstance()
        val message = getString(R.string.app_term_privacy)
        val s2 = getString(R.string.app_terms_of_use)
        val s1 = getString(R.string.app_non_privacy_policy)
        val wordtoSpan: Spannable = SpannableString(message)
        wordtoSpan.setSpan(object : ClickableSpan() {
            override fun onClick(view: View) {
                onPolicy()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = resources.getColor(R.color.text_link)
                ds.isUnderlineText
            }
        }, message.indexOf(s1), message.indexOf(s1) + s1.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        wordtoSpan.setSpan(object : ClickableSpan() {
            override fun onClick(view: View) {
                onPolicy()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = resources.getColor(R.color.text_link)
                ds.isUnderlineText
            }
        }, message.indexOf(s2), message.indexOf(s2) + s2.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tv_declaration!!.text = wordtoSpan
    }

    private fun onPolicy() {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(PRIVACY_POLICY)
        )
        startActivity(browserIntent)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setHasOptionsMenu(true)
    }

    private fun initInAppPurchase() {

        try {
            billingClient = BillingClient.newBuilder(requireContext())
                .setListener(purchaseUpdateListener)
                .enablePendingPurchases()
                .build()
            billingClient!!.startConnection(object : BillingClientStateListener {

                override fun onBillingServiceDisconnected() {
                    Log.e("TAG_", "onBillingServiceDisconnected::::: ")
                }

                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    Log.e(
                        "TAG_",
                        "onBillingSetupFinished:::: " + billingResult.responseCode + " " + BillingClient.BillingResponseCode.OK
                    )
                    checkSubscriptionList()
                }
            })
        } catch (e: java.lang.Exception) {
            Log.d("TAG_ERROR", "initInAppPurchase: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }

    private val purchaseUpdateListener: PurchasesUpdatedListener =
        PurchasesUpdatedListener { result, _ ->
            try {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                        requireActivity().getSharedPrefs().edit().putBoolean(PREF_KEY_PURCHASE_STATUS, true).apply()
                        dismiss()
                    }
                } else {
                    requireActivity().getSharedPrefs().edit().putBoolean(PREF_KEY_PURCHASE_STATUS, true).apply()
                    dismiss()
                }
                checkSubscriptionList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    private fun checkSubscriptionList() {
        if (billingClient != null) {
            var isPurchasedSku = false
            try {
                billingClient!!.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                ) { purchasesResult, purchaseList ->
                    Log.d("TAG_CODE", "checkSubscriptionList: ${purchasesResult.responseCode}")
                    if (purchasesResult.responseCode == 0) {

                        if (purchaseList.isNotEmpty()) {
                            for (i in 0 until purchaseList.size) {
                                val purchaseData = purchaseList[i]
                                Log.d("TAG_PURCHASE", "checkSubscriptionList: $purchaseData")
                                if ((purchaseData.products.contains(MONTHLY_SKU)) || (purchaseData.products.contains(
                                        YEARLY_SKU
                                    )) || (purchaseData.products.contains(WEEKLY_SKU))
                                ) {
                                    isPurchasedSku = true
                                }

                                if (purchaseData.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                    if (!purchaseData.isAcknowledged) {
                                        val acknowledgePurchaseParams =
                                            AcknowledgePurchaseParams.newBuilder()
                                                .setPurchaseToken(purchaseData.purchaseToken)
                                        billingClient!!.acknowledgePurchase(
                                            acknowledgePurchaseParams.build()
                                        ) { p0 ->
                                            Log.e("BillingResult ======>", p0.debugMessage)
                                        }
                                    }
                                }
                            }
                        }
                        requireActivity().getSharedPrefs().edit().putBoolean(PREF_KEY_PURCHASE_STATUS, isPurchasedSku).apply()
                        getSKUDetails()
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

        }
    }


    @SuppressLint("SetTextI18n")
    private fun getSKUDetails() {
        val productListMonth =
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(WEEKLY_SKU)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(MONTHLY_SKU)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(YEARLY_SKU)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            )

        val paramsNewMonth = QueryProductDetailsParams.newBuilder().setProductList(productListMonth)

        billingClient!!.queryProductDetailsAsync(paramsNewMonth.build()) { billingResult,
                                                                           skuDetailsList ->

            Log.d("TAG_SKU_LIST", "getSKUDetails: ${skuDetailsList.size} ${billingResult.responseCode} $progressBar")
            if (billingResult.responseCode == 0 && skuDetailsList.isNotEmpty()) {
                try {
                    requireActivity().runOnUiThread {
                        Log.d("TAG_PRODUCT", "getSKUDetails: ${Gson().toJson(skuDetailsList)}")
                        if (skuDetailsList.isNotEmpty()) {
                            val temp = skuDetailsList[0]
                            skuDetailsList[0] = skuDetailsList[1]
                            skuDetailsList[1] = temp
                            subscriptionPlanAdapter.updateList(skuDetailsList)
                            progressBar?.isVisible = false
                        }


                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        if (billingClient != null) {
            billingClient!!.endConnection()
        }
    }


}
