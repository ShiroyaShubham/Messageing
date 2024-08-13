package messenger.messages.messaging.sms.chat.meet.dialogs

import android.app.Activity
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import messenger.messages.messaging.sms.chat.meet.extensions.beVisibleIf
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.R.id.*
import messenger.messages.messaging.sms.chat.meet.extensions.mPref
import messenger.messages.messaging.sms.chat.meet.extensions.setupDialogStuff
import messenger.messages.messaging.sms.chat.meet.utils.CONFLICT_KEEP_BOTH
import messenger.messages.messaging.sms.chat.meet.utils.CONFLICT_MERGE
import messenger.messages.messaging.sms.chat.meet.utils.CONFLICT_OVERWRITE
import messenger.messages.messaging.sms.chat.meet.utils.CONFLICT_SKIP
import messenger.messages.messaging.sms.chat.meet.model.FileDIRModel

class FileConflictDialog(val mActivity: Activity, val fileDirItem: FileDIRModel, val showApplyToAllCheckbox: Boolean,
                         val callback: (resolution: Int, applyForAll: Boolean) -> Unit) {
    val view = mActivity.layoutInflater.inflate(R.layout.app_d_file_conflict, null)!!

    init {
        view.apply {
            val stringBase = if (fileDirItem.isDirectory) R.string.folder_already_exists else R.string.file_already_exists
            findViewById<TextView>(conflict_dialog_title).text = String.format(mActivity.getString(stringBase), fileDirItem.name)
            findViewById<CheckBox>(conflict_dialog_apply_to_all).isChecked = mActivity.mPref.lastConflictApplyToAll
            findViewById<CheckBox>(conflict_dialog_apply_to_all).beVisibleIf(showApplyToAllCheckbox)
            findViewById<RadioButton>(conflict_dialog_radio_merge).beVisibleIf(fileDirItem.isDirectory)

            val resolutionButton = when (mActivity.mPref.lastConflictResolution) {
                CONFLICT_OVERWRITE -> findViewById<RadioButton>(conflict_dialog_radio_overwrite)
                CONFLICT_MERGE -> findViewById(conflict_dialog_radio_merge)
                else -> findViewById(conflict_dialog_radio_skip)
            }
            resolutionButton.isChecked = true
        }

        AlertDialog.Builder(mActivity)
                .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    mActivity.setupDialogStuff(view, this)
                }
    }

    private fun dialogConfirmed() {
        val resolution = when (view.findViewById<RadioGroup>(conflict_dialog_radio_group).checkedRadioButtonId) {
            conflict_dialog_radio_skip -> CONFLICT_SKIP
            conflict_dialog_radio_merge -> CONFLICT_MERGE
            conflict_dialog_radio_keep_both -> CONFLICT_KEEP_BOTH
            else -> CONFLICT_OVERWRITE
        }

        val applyToAll = view.findViewById<CheckBox>(conflict_dialog_apply_to_all).isChecked
        mActivity.mPref.apply {
            lastConflictApplyToAll = applyToAll
            lastConflictResolution = resolution
        }

        callback(resolution, applyToAll)
    }
}
