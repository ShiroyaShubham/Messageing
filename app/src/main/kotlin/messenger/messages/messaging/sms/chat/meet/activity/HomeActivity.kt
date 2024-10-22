package messenger.messages.messaging.sms.chat.meet.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.adsdk.plugin.AdsUtils
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import me.zhanghai.android.materialprogressbar.IndeterminateHorizontalProgressDrawable
import messenger.messages.messaging.sms.chat.meet.MainAppClass.Companion.setupSIMSelector
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityHomeBinding
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.fragment.*
import messenger.messages.messaging.sms.chat.meet.model.LoadConversationsModel
import messenger.messages.messaging.sms.chat.meet.subscription.InAppPurchaseDialogFragment
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass
import messenger.messages.messaging.sms.chat.meet.utils.*
import org.greenrobot.eventbus.EventBus


class HomeActivity : BaseHomeActivity() {
    lateinit var binding: ActivityHomeBinding
    var pagerAdapter: ViewPagerAdapter? = null
    var doubleBackToExitPressedOnce = false


    override fun onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer()
            return
        }
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            showExitDialog(this)
        }
    }

    private fun openDrawer() {
        findViewById<DrawerLayout>(R.id.mDrawer).openDrawer(GravityCompat.START)
    }

    private fun closeDrawer() {
        findViewById<DrawerLayout>(R.id.mDrawer).closeDrawer(GravityCompat.START)
    }


    private fun isDrawerOpen() = findViewById<DrawerLayout>(R.id.mDrawer).isDrawerOpen(GravityCompat.START)

//    private fun getBanner() {
//        if (isInternetAvailable(this)) {
//
////            WebApiClient.getInstance().GetAdvertisement().enqueue(object : Callback<AdModel> {
////                override fun onResponse(call: Call<AdModel>, response: Response<AdModel>) {
////                    Log.e("TAG_RESPONSE", "onResponse:>>>>> " + Gson().toJson(response.body()))
////                    if (response.code() == 200 && response.body()!!.success) {
//////                        Log.e("TAG", "onResponse:>>>>> "+response.message().toString() )
////
////
////                        getSharedPrefs().edit().putString(
////                            APP_OPEN_AD_ID,
////                            response.body()!!.getData().getAds().getAdmob().getApp_open()
////                        ).apply()
////                        getSharedPrefs().edit().putString(
////                            BANNER_AD_ID,
////                            response.body()!!.getData().getAds().getAdmob().getBanner()
////                        ).apply()
////                        getSharedPrefs().edit().putString(
////                            NATIVE_AD_ID,
////                            response.body()!!.getData().getAds().getAdmob().getNativeBanner()
////                        ).apply()
////                        getSharedPrefs().edit().putString(
////                            INTERSTITIAL_AD_ID,
////                            response.body()!!.getData().getAds().getAdmob().getInterstitial()
////                        ).apply()
////                        getSharedPrefs().edit().putString(
////                            AD_STATUS,
////                            response.body()!!.getData().getAds().getSetting().getAd_show_status()
////                        ).apply()
////                        getSharedPrefs().edit().putString(
////                            REWARD_AD,
////                            response.body()!!.getData().getAds().getAdmob().getRewarded_video()
////                        ).apply()
////
//////                        Utils.setPref(
//////                            this,
//////                            .PRIVACY_POLICY,
//////                            response.body()!!.getData().getAds().getSetting()
//////                                .getPrivacy_policy_link()
//////                        )

////                    }
////                }
////
////                override fun onFailure(call: Call<AdModel>, t: Throwable) {
////                    Log.d("TAG_ERROR", "onFailure: ${t.message}")
////
////                }
////            })
//            showBannerAds(findViewById(R.id.mBannerAdsContainer))
//        }
//    }

    private fun isInternetAvailable(context: Context?): Boolean {
        return if (context != null) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            // test for connection
            (cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isAvailable
                && cm.activeNetworkInfo!!.isConnected)
        } else false
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.indeterminateDrawable = IndeterminateHorizontalProgressDrawable(this)
        window.statusBarColor = resources.getColor(R.color.screen_background_color)
        SharedPrefrenceClass.getInstance()!!.setBoolean("isSplash", false)
        setupSIMSelector()
        bindNavHandlers()
        binding.fabContactList.setOnClickListener {
            Intent(this, ContactsActivity::class.java).apply {
                startActivity(this)
            }
        }
        binding.tvSearch.setOnClickListener {
            val intent = Intent(this, SearchMessagesActivity::class.java)
            startActivity(
                intent,
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    binding.linSearch, getString(R.string.app_name)
                ).toBundle()
            )
        }
        binding.ivProCrown.setOnClickListener {

            onViewPurchaseDialog()
        }
        binding.imgMenu.setOnClickListener {
            openDrawer()
        }


        appLaunched(packageName)

        binding.txtVersion.text = "V.${packageManager.getPackageInfo(packageName, 0).versionName}"
        pagerAdapter = ViewPagerAdapter(supportFragmentManager, this)
        pagerAdapter!!.addFrag(AllSMSFragment().newInstance(), R.drawable.ic_tab_all, getString(R.string.app_all))
        pagerAdapter!!.addFrag(PersonalFragment().newInstance(), R.drawable.ic_tab_personal, getString(R.string.app_personal))
        pagerAdapter!!.addFrag(OTPsFragment().newInstance(), R.drawable.ic_tag_otp, getString(R.string.app_otps))
        pagerAdapter!!.addFrag(TransactionsFragment().newInstance(), R.drawable.icon_tab_transactions, getString(R.string.app_transactions))
        pagerAdapter!!.addFrag(OffersFragment().newInstance(), R.drawable.icon_tab_offers, getString(R.string.app_offers))
        binding.viewPager.adapter = pagerAdapter

        binding.tabLayout.setTabRippleColorResource(android.R.color.transparent)
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                Log.e("onTabSelected: ", "" + tab.position)
                setCurrentTab(tab.position)
                if (tab.position == 0) {
                    val fragmet = pagerAdapter!!.getItem(tab.position) as AllSMSFragment
                    fragmet.newInstance2()
                    binding.llSyncMsgProgress.isVisible = !getSharedPrefs().getBoolean(IS_FETCH_ALL_CONVERSATION,false)
                }

                if (tab.position == 1) {
                    val fragmet = pagerAdapter!!.getItem(tab.position) as PersonalFragment
                    binding.llSyncMsgProgress.isVisible = !getSharedPrefs().getBoolean(IS_FETCH_PERSONAL_CONVERSATION,false)
//                    fragmet.newInstance2()
                }
                if (tab.position == 2) {
                    val fragmet = pagerAdapter!!.getItem(tab.position) as OTPsFragment
//                    fragmet.newInstance2()
                    binding.llSyncMsgProgress.isVisible = !getSharedPrefs().getBoolean(IS_FETCH_ALL_CONVERSATION,false)

                }
                if (tab.position == 3) {
                    val fragmet = pagerAdapter!!.getItem(tab.position) as TransactionsFragment
                    binding.llSyncMsgProgress.isVisible = !getSharedPrefs().getBoolean(IS_FETCH_ALL_CONVERSATION,false)
//                    fragmet.newInstance2()

                }
                if (tab.position == 4) {
                    val fragmet = pagerAdapter!!.getItem(tab.position) as OffersFragment
                    binding.llSyncMsgProgress.isVisible = !getSharedPrefs().getBoolean(IS_FETCH_ALL_CONVERSATION,false)
//                    fragmet.newInstance2()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        for (i in 0 until binding.tabLayout.tabCount) {
            val tab = binding.tabLayout.getTabAt(i)
            tab!!.customView = pagerAdapter!!.getTabView(i)
        }
        setCurrentTab(0)


        inAppUpdate()

        if (!PrefClass.isProUser) {
            binding.ivProCrown.visibility = View.VISIBLE
        } else {
            binding.ivProCrown.visibility = View.GONE
        }
        SharedPrefrenceClass.getInstance()?.setInt(
            OPEN_IN_APP_PURCHASE_DIALOG_COUNT,
            SharedPrefrenceClass.getInstance()?.getInt(OPEN_IN_APP_PURCHASE_DIALOG_COUNT, 0)!! + 1
        )
    }


    private fun bindNavHandlers() {
        binding.txtSetting.setOnClickListener {
            closeDrawer()
            showInterstitialAdPerDayOnDrawerItemClick {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        binding.txtBackupAndRestore.setOnClickListener {
            closeDrawer()
            showInterstitialAdPerDayOnDrawerItemClick {
                startActivity(Intent(this, BackupRestoreActivity::class.java))
            }
        }
        binding.txtBlock.setOnClickListener {
            closeDrawer()
            showInterstitialAdPerDayOnDrawerItemClick {
                startActivity(Intent(this, BlockedNumberActivity::class.java))
            }
        }
        binding.txtInbox.setOnClickListener {
            closeDrawer()
            showInterstitialAdPerDayOnDrawerItemClick { }
        }
        binding.txtLanguage.setOnClickListener {
            closeDrawer()
            showInterstitialAdPerDayOnDrawerItemClick {
                startActivity(Intent(this, LanguageActivity::class.java))
            }
        }
        binding.txtArchived.setOnClickListener {
            closeDrawer()
            showInterstitialAdPerDayOnDrawerItemClick {
                startActivity(Intent(this, ArchivedMessageActivity::class.java))
            }
        }
        binding.txtRateUs.setOnClickListener {
            closeDrawer()
            showInterstitialAdPerDayOnDrawerItemClick {
                launchViewIntent("https://play.google.com/store/apps/details?id=${getStringsPackageName()}")
            }
        }
        binding.txtPrivacyPolicy.setOnClickListener {
            closeDrawer()
            showInterstitialAdPerDayOnDrawerItemClick {
//                launchViewIntent(getPolicyLink())
                policy(getPolicyLink(), "Privacy Policy can't open")
            }
        }
    }

    private fun policy(url: String, msg: String) {
        try {
            val marketUri = Uri.parse(url)
            val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
            startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this@HomeActivity, msg, Toast.LENGTH_SHORT).show()
            //DO Something
        }
    }



    private fun onViewPurchaseDialog() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val previous = supportFragmentManager.findFragmentByTag(HomeActivity::class.java.name)
        if (previous != null) {
            fragmentTransaction.remove(previous)
        }
        fragmentTransaction.addToBackStack(null)
        val proActivity = InAppPurchaseDialogFragment()
        proActivity.show(fragmentTransaction, InAppPurchaseDialogFragment::class.java.name)
    }

    private val MAKE_DEFAULT_APP_REQUEST = 1
    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            showErrorToast("Done")
                            toast("All Permission Given")
                            setupSIMSelector()
                            EventBus.getDefault().post(LoadConversationsModel())
                        }
                    } else {
                        toast(getString(R.string.unknown_error_occurred))
                        finish()
                    }
                }
            } else {
                toast(getString(R.string.unknown_error_occurred))
                finish()
            }
        }
    }


    companion object {
        const val TAG2 = "AppUpdate"
        const val RC_APP_UPDATE = 11
    }

    override fun onStop() {
        super.onStop()
    }

    private fun inAppUpdate() {
        mAppUpdateManager = AppUpdateManagerFactory.create(this)
        mAppUpdateManager!!.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                Log.e(TAG2, "OnSuccess")
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE /*AppUpdateType.IMMEDIATE*/)
                ) {
                    try {
                        mAppUpdateManager!!.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this, RC_APP_UPDATE
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate()
                } else {
                    Log.e(
                        TAG2, "checkForAppUpdateAvailability: something else"
                    )
                }
            }
    }

    private fun popupSnackbarForCompleteUpdate() {
        val snackbar = Snackbar.make(
            binding.relParent!!,
            "New app is ready!",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.setAction("Install") {
            if (mAppUpdateManager != null) {
                mAppUpdateManager!!.completeUpdate()
            }
        }
        snackbar.setActionTextColor(resources.getColor(R.color.whiteColor))
        snackbar.show()
    }


    override fun onResume() {
        super.onResume()
        try {
            mAppUpdateManager!!.appUpdateInfo
                .addOnSuccessListener { result ->
                    if (result.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        // If an in-app update is already running, resume the update.
                        try {
                            mAppUpdateManager!!.startUpdateFlowForResult(
                                result,
                                AppUpdateType.IMMEDIATE,
                                this, RC_APP_UPDATE
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            e.printStackTrace()
                        }
                    }
                }
            mAppUpdateManager!!.appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate()
                    }
                }
            if (!PrefClass.isProUser){
            showBannerAds(findViewById(R.id.mBannerAdsContainer))
            }else{
                findViewById<ViewGroup>(R.id.mBannerAdsContainer)?.visibility = View.GONE
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    private var mAppUpdateManager: AppUpdateManager? = null


    inner class ViewPagerAdapter(manager: FragmentManager?, val conttex: Context) : FragmentPagerAdapter(manager!!) {
        private val mFragmentList: MutableList<Fragment> = ArrayList()
        private val mFragmentTitleList: MutableList<String> = ArrayList()
        private val mFragmentIconList: MutableList<Int> = ArrayList()


        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFrag(fragment: Fragment, icon: Int, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
            mFragmentIconList.add(icon)
        }

        fun getTabView(position: Int): View {
            val tabView: View = LayoutInflater.from(conttex).inflate(R.layout.layout_home_tabs, null)
            (tabView.findViewById<View>(R.id.tvTopTitle) as TextView).text = mFragmentTitleList[position]
            (tabView.findViewById<View>(R.id.tvTopTitle) as TextView)
                .setTextColor(resources.getColor(R.color.whiteColor))
            Glide.with(this@HomeActivity).load(mFragmentIconList[position])
                .into((tabView.findViewById<View>(R.id.ivTabThumb) as ImageView))
            (tabView.findViewById<View>(R.id.tvTopTitle) as TextView)
                .setTextColor(resources.getColor(R.color.whiteColor))
            (tabView.findViewById<View>(R.id.linTabParent) as LinearLayout)
                .setBackgroundResource(R.drawable.bg_gradient_round_corner_25)

            return tabView
        }

        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitleList[position]
        }
    }


    private fun setCurrentTab(tabPos: Int) {
        for (i in 0 until binding.tabLayout.tabCount) {
            val tabView: View = binding.tabLayout.getTabAt(i)!!.customView!!
            val tvTopTitle = tabView.findViewById<TextView>(R.id.tvTopTitle)
            val linTabParent = tabView.findViewById<LinearLayout>(R.id.linTabParent)
            val ivTabThumb = tabView.findViewById<ImageView>(R.id.ivTabThumb)

            if (i == tabPos) {
                tvTopTitle.setTextColor(resources.getColor(R.color.white))
                ivTabThumb.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.SRC_ATOP)
                linTabParent.setBackgroundResource(R.drawable.bg_gradient_round_corner_5)
            } else {
                tvTopTitle.setTextColor(resources.getColor(R.color.dark_grey))
                ivTabThumb.setColorFilter(resources.getColor(R.color.dark_grey), android.graphics.PorterDuff.Mode.SRC_ATOP)
                linTabParent.setBackgroundResource(R.drawable.bg_grey_round_corner_5)
            }
            binding.tabLayout.getTabAt(i)!!.customView = tabView
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == MAKE_DEFAULT_APP_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Log.e("Event: ", "askPermissions RESULT_OK")
                askPermissions()
            } else {
                toast(getString(R.string.unknown_error_occurred))
                finish()
            }
        } else if (requestCode == RC_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Log.e(
                    TAG2,
                    "onActivityResult: app download failed"
                )
            }
        }
    }
}

