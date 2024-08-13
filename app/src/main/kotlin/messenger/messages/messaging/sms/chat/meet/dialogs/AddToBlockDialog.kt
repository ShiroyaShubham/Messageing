package messenger.messages.messaging.sms.chat.meet.dialogs

import android.app.Dialog
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import messenger.messages.messaging.sms.chat.meet.extensions.addBlockedNumber
import messenger.messages.messaging.sms.chat.meet.extensions.deleteBlockedNumber
import messenger.messages.messaging.sms.chat.meet.extensions.value
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseActivity
import messenger.messages.messaging.sms.chat.meet.model.BlockContactModel
import messenger.messages.messaging.sms.chat.meet.model.BlockedNumberModel

class AddToBlockDialog(val mActivity: BaseActivity, val originalNumber: BlockContactModel? = null, val callback: () -> Unit) {

    init {
        val dialog = Dialog(mActivity)
        dialog.setContentView(R.layout.dialog_add_to_block)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<EditText>(R.id.etAddNumber).apply {
            requestFocus()
            if (originalNumber != null) {
                setText(originalNumber.number)
            }
        }

        dialog.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<TextView>(R.id.tvOk).setOnClickListener {
            val newBlockedNumber = dialog.findViewById<EditText>(R.id.etAddNumber).value
            if (originalNumber != null && newBlockedNumber != originalNumber.number) {
                mActivity.deleteBlockedNumber(originalNumber.blockID)
            }

            if (newBlockedNumber.isNotEmpty()) {
                mActivity.addBlockedNumber(newBlockedNumber)
            }

            callback()
            dialog.dismiss()
        }
        if (!dialog.isShowing) dialog.show()
    }
}
