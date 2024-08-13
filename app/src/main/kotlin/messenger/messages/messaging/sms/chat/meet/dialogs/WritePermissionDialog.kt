package messenger.messages.messaging.sms.chat.meet.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseActivity
import messenger.messages.messaging.sms.chat.meet.extensions.setupDialogStuff

class WritePermissionDialog(mActivity: Activity, val isOTG: Boolean, val callback: () -> Unit) {
    var dialog: AlertDialog

    init {
        val layout = if (isOTG) R.layout.app_d_write_permission_otg else R.layout.app_d_write_permission
        val view = mActivity.layoutInflater.inflate(layout, null)

        val glide = Glide.with(mActivity)
        val crossFade = DrawableTransitionOptions.withCrossFade()
        if (isOTG) {
            glide.load(R.drawable.img_write_storage_otg).transition(crossFade).into(view.findViewById(R.id.write_permissions_dialog_otg_image))
        } else {
            glide.load(R.drawable.img_write_storage).transition(crossFade).into(view.findViewById(R.id.write_permissions_dialog_image))
            glide.load(R.drawable.img_write_storage_sd).transition(crossFade).into(view.findViewById(R.id.write_permissions_dialog_image_sd))
        }

        dialog = AlertDialog.Builder(mActivity)
                .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
                .setOnCancelListener {
                    BaseActivity.funAfterSAFPermission?.invoke(false)
                    BaseActivity.funAfterSAFPermission = null
                }
                .create().apply {
                    mActivity.setupDialogStuff(view, this, R.string.confirm_storage_access_title)
                }
    }

    private fun dialogConfirmed() {
        dialog.dismiss()
        callback()
    }
}
