package messenger.messages.messaging.sms.chat.meet.subscription

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.utils.SharedPrefrenceClass

class PrefClass {


    companion object {
        const val IS_PRO_USER = "is_pro_user"


        var isProUser: Boolean
            get() = SharedPrefrenceClass.getInstance()!!.getBoolean(IS_PRO_USER, false)
            set(rate) { SharedPrefrenceClass.getInstance()!!.setBoolean(IS_PRO_USER, rate)
            }


        fun showProDialog(act: Activity, listner: OnProDialogClickListner) {
            Log.d("photo_art_Event--", "Act_Main_proDialog")
            val exitDialog = Dialog(act)
            exitDialog.window?.setBackgroundDrawable(ColorDrawable(0))
            exitDialog.setContentView(R.layout.dialog_pro)
            exitDialog.window?.setGravity(Gravity.BOTTOM)
            exitDialog.setCancelable(true)
            exitDialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            val close: ImageView = exitDialog.findViewById(R.id.close)
            val linPremium: LinearLayout = exitDialog.findViewById(R.id.lin_premium)

            close.setOnClickListener {
                try {
                    exitDialog.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            linPremium.setOnClickListener {
                exitDialog.dismiss()
                listner.onViewPurchaseDialog()
            }
            exitDialog.show()

        }


        fun hasNavBar(context: Context): Boolean {
            val resources = context.resources
            val id = resources.getIdentifier("config_showNavigationBar", "bool", "android")
            return if (id > 0) {
                resources.getBoolean(id)
            } else {    // Check for keys
                val hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey()
                val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
                !hasMenuKey && !hasBackKey
            }
        }

    }


    interface OnProDialogClickListner{
        fun onViewPurchaseDialog()
    }
}
