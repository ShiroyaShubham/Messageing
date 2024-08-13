package messenger.messages.messaging.sms.chat.meet.dialogs

import android.app.Dialog
import android.content.ContextWrapper
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import messenger.messages.messaging.sms.chat.meet.extensions.getCurrentFormattedDateTime
import messenger.messages.messaging.sms.chat.meet.extensions.isAValidFilename
import messenger.messages.messaging.sms.chat.meet.extensions.toast
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.utils.config
import messenger.messages.messaging.sms.chat.meet.utils.EXPORT_FILE_EXT
import messenger.messages.messaging.sms.chat.meet.views.GradientTextView
import java.io.File
import java.util.*

class ExportDialog(
    private val mActivity: BaseHomeActivity,
    private val callback: (file: File) -> Unit,
) {
    val date = mActivity.getCurrentFormattedDateTime()

    private val config = mActivity.config

    val cw = ContextWrapper(mActivity)
    val directory: File = cw.filesDir

    init {

        val dialog = Dialog(mActivity)
        dialog.setContentView(R.layout.dialog_conformation)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(true)
        val width = (mActivity.resources.displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.setLayout(
            width, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvExportCancel = dialog.findViewById<TextView>(R.id.tvAlertCancel)
        val tvExportOk = dialog.findViewById<GradientTextView>(R.id.tvAlertOk)
        tvExportCancel.setOnClickListener {
            dialog.dismiss()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tvExportOk.setGradientColors(mActivity.getColor(R.color.blue),mActivity.getColor(R.color.purple))
        }
        tvExportOk.setOnClickListener {
            val filename = "${mActivity.getString(R.string.app_message)}_${date}"
            when {
                filename.isEmpty() -> mActivity.toast(R.string.empty_name)
                filename.isAValidFilename() -> {
                    val file = File(directory, "$filename$EXPORT_FILE_EXT")
                    if (file.exists()) {
                        mActivity.toast(R.string.name_taken)
                        return@setOnClickListener
                    }

                    /* if (!ckExportSMS.isChecked && !ckExportMMS.isChecked) {
                         mActivity.toast(R.string.no_option_selected)
                         return@setOnClickListener
                     }*/
                    val currentTime: Date = Calendar.getInstance().time
                    config.exportSms = true
                    config.lastExportDate = currentTime.toString().trim()
                    callback(file)
                    dialog.dismiss()
                }
                else -> mActivity.toast(R.string.invalid_name)
            }
        }

        if (!dialog.isShowing) dialog.show()

    }
}
