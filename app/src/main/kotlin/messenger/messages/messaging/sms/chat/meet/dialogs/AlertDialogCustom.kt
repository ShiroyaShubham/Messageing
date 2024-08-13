package messenger.messages.messaging.sms.chat.meet.dialogs

import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.views.GradientTextView

class AlertDialogCustom(
    mActivity: Activity, title: String = "", message: String = "",
    val callback: () -> Unit
) {

    init {
        val dialog = Dialog(mActivity)
        dialog.setContentView(R.layout.dialog_alert)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(true)
        val width = (mActivity.resources.displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.setLayout(
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvAlertTitle = dialog.findViewById<TextView>(R.id.tvAlertTitle)
        val tvAlertDesc = dialog.findViewById<TextView>(R.id.tvAlertDesc)
        val tvAlertCancel = dialog.findViewById<TextView>(R.id.tvAlertCancel)
        val tvAlertOk = dialog.findViewById<GradientTextView>(R.id.tvAlertOk)

        tvAlertTitle.text = title
        tvAlertDesc.text =  message
        tvAlertOk.text = title
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tvAlertOk.setGradientColors(mActivity.getColor(R.color.blue),mActivity.getColor(R.color.purple))
        }

        tvAlertCancel.setOnClickListener {
            dialog.dismiss()
        }

        tvAlertOk.setOnClickListener {
            dialog.dismiss()
            callback()
        }
        if (!dialog.isShowing) dialog.show()
    }
}
