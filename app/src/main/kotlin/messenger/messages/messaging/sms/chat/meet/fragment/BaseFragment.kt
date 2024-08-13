package messenger.messages.messaging.sms.chat.meet.fragment

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import messenger.messages.messaging.sms.chat.meet.ads.AdsManager
import messenger.messages.messaging.sms.chat.meet.extensions.getPermissionString
import messenger.messages.messaging.sms.chat.meet.extensions.getSharedPrefs
import messenger.messages.messaging.sms.chat.meet.extensions.hasPermission
import messenger.messages.messaging.sms.chat.meet.listners.AdsDismissCallback
import messenger.messages.messaging.sms.chat.meet.model.*
import messenger.messages.messaging.sms.chat.meet.utils.CircularProgressDialog
import messenger.messages.messaging.sms.chat.meet.utils.LAST_AD_DISPLAY_MESSAGE_CLICK
import messenger.messages.messaging.sms.chat.meet.utils.ONE_DAY_IN_MILLIS
import messenger.messages.messaging.sms.chat.meet.utils.isOnMainThread
import java.util.Calendar


open class BaseFragment : Fragment() {
    var actionOnPermission: ((granted: Boolean) -> Unit)? = null
    var isAskingPermissions = false
    private val GENERIC_PERM_HANDLER = 100
    var mActivity: Activity? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity)
            mActivity = context
    }

    fun toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
        try {
            if (isOnMainThread()) {
                doToast(requireContext(), msg, length)
            } else {
                Handler(Looper.getMainLooper()).post {
                    doToast(requireContext(), msg, length)
                }
            }
        } catch (e: Exception) {
            Log.i("Exception", e.toString())
        }
    }

    private fun doToast(context: Context, message: String, length: Int) {
        if (context is Activity) {
            if (!context.isFinishing && !context.isDestroyed) {
                Toast.makeText(context, message, length).show()
            }
        } else {
            Toast.makeText(context, message, length).show()
        }
    }

    fun handlePermission(permissionId: Int, callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (requireActivity().hasPermission(permissionId)) {
            callback(true)
        } else {
            isAskingPermissions = true
            actionOnPermission = callback
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(requireActivity().getPermissionString(permissionId)), GENERIC_PERM_HANDLER)
        }
    }

    fun checkIsInList(phoneNumber: String, messagesNew: ArrayList<MessagesModel>): Boolean {
        for (contactNumber in messagesNew) {
            if (phoneNumber.contains(contactNumber.participants[0].phoneNumbers.toString())) {
                return true
            }
        }
        return false
    }

    fun removeCommonItems(list1: MutableList<ConversationSmsModel>, list2: List<ArchivedModel>): ArrayList<ConversationSmsModel> {
        list1.removeAll { item1 ->
            list2.any { item2 ->
                // Customize this condition according to your object comparison logic
                val nonArchiveNo = if (item1.phoneNumber.startsWith("91")) "+${item1.phoneNumber}" else item1.phoneNumber
                val archivedNum = if (item2.number.startsWith("91")) "+${item2.number}" else item2.number
                nonArchiveNo == archivedNum // Assuming 'id' is a property used for comparison
            }
        }

        return list1 as ArrayList<ConversationSmsModel>
    }

    fun removeArchiveItem(list1: MutableList<SearchModel>, list2: List<ArchivedModel>): ArrayList<SearchModel> {
        list1.removeAll { item1 ->
            list2.any { item2 ->
                Log.d("TAG_ARCHIVED", "removeArchiveItem: ${item1.phoneNumber} ${item2.number}")
                // Customize this condition according to your object comparison logic
                val nonArchiveNo = if (item1.phoneNumber!!.startsWith("91")) "+${item1.phoneNumber}" else item1.phoneNumber
                val archivedNum = if (item2.number.startsWith("91")) "+${item2.number}" else item2.number
                nonArchiveNo == archivedNum // Assuming 'id' is a property used for comparison
            }
        }

        return list1 as ArrayList<SearchModel>
    }

    fun removeBlockItem(list1: MutableList<SearchModel>, list2: List<BlockContactModel>): ArrayList<SearchModel> {
        list1.removeAll { item1 ->
            list2.any { item2 ->
                // Customize this condition according to your object comparison logic
                val nonBlockNo = if (item1.phoneNumber!!.startsWith("91")) "+${item1.phoneNumber}" else item1.phoneNumber
                val blockNo = if (item2.number.startsWith("91")) "+${item2.number}" else item2.number
                Log.d("TAG_BLOCK_ITEM", "removeBlockItem: $nonBlockNo - $blockNo")
                nonBlockNo == blockNo // Assuming 'id' is a property used for comparison
            }
        }

        return list1 as ArrayList<SearchModel>
    }

    fun showInterstitialAdPerDayOnMessageClick(onItemClick: () -> Unit) {
        val lastAdDisplayTime = requireContext().getSharedPrefs().getLong(LAST_AD_DISPLAY_MESSAGE_CLICK, 0L)
        val currentTime = Calendar.getInstance().timeInMillis
        if (currentTime - lastAdDisplayTime >= ONE_DAY_IN_MILLIS) {
            showDialog()
            AdsManager.loadInterstitialAds(requireActivity(), object : AdsDismissCallback {
                override fun onAdDismiss() {
                    dismissDialog()
                    onItemClick.invoke()
                }
            })
            requireContext().getSharedPrefs().edit().putLong(LAST_AD_DISPLAY_MESSAGE_CLICK, currentTime).apply()
        } else {
            dismissDialog()
            onItemClick.invoke()
        }
    }

    var ad: CircularProgressDialog? = null

    fun showDialog() {
        try {
            if (ad != null && ad!!.isShowing) {
                return
            }
            if (ad == null) {
                ad = CircularProgressDialog.getInstant(requireActivity())
            }
            ad!!.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissDialog() {
        try {
            if (ad != null) {
                ad!!.dismiss()
                ad = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun isInternetAvailable(context: Context?): Boolean {
        return if (context != null) {
            val cm = context.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
            // test for connection
            if (cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isAvailable
                && cm.activeNetworkInfo!!.isConnected
            ) {
                true
            } else {
                false
            }
        } else false
    }

}
