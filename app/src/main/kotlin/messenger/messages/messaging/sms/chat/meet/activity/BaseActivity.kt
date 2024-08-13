package messenger.messages.messaging.sms.chat.meet.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.ads.AdsManager
import messenger.messages.messaging.sms.chat.meet.databinding.DialogExitBinding
import messenger.messages.messaging.sms.chat.meet.databinding.LayoutLoadingBinding
import messenger.messages.messaging.sms.chat.meet.dialogs.FileConflictDialog
import messenger.messages.messaging.sms.chat.meet.dialogs.WritePermissionDialog
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.listners.AdsDismissCallback
import messenger.messages.messaging.sms.chat.meet.model.*
import messenger.messages.messaging.sms.chat.meet.utils.*
import messenger.messages.messaging.sms.chat.meet.utils.MyContextWrapperUtils
import java.io.OutputStream
import java.util.*
import java.util.regex.Pattern

abstract class BaseActivity : AppCompatActivity() {
    var copyMoveCallback: ((destinationPath: String) -> Unit)? = null
    var actionOnPermission: ((granted: Boolean) -> Unit)? = null
    var isAskingPermissions = false
    var useDynamicTheme = true
    var checkedDocumentPath = ""
    var configItemsToExport = LinkedHashMap<String, Any>()
    var loadingDialog: Dialog? = null

    private val GENERIC_PERM_HANDLER = 100
    private val DELETE_FILE_SDK_30_HANDLER = 300
    private val RECOVERABLE_SECURITY_HANDLER = 301

    companion object {
        var funAfterSAFPermission: ((success: Boolean) -> Unit)? = null
        var funAfterDelete30File: ((success: Boolean) -> Unit)? = null
        var funRecoverableSecurity: ((success: Boolean) -> Unit)? = null
    }

    abstract fun getAppIconIDs(): ArrayList<Int>

    abstract fun getAppLauncherName(): String

    override fun onDestroy() {
        super.onDestroy()
        funAfterSAFPermission = null
        actionOnPermission = null
    }

//    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
//        super.onCreate(savedInstanceState, persistentState)
//        initLoadingDialog(this)
//    }

    override fun onStart() {
        super.onStart()
        initLoadingDialog(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun attachBaseContext(newBase: Context) {
        if (newBase.mPref.useEnglish) {
            super.attachBaseContext(MyContextWrapperUtils(newBase).wrap(newBase, "en"))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        val partition = try {
            checkedDocumentPath.substring(9, 18)
        } catch (e: Exception) {
            ""
        }

        val sdOtgPattern = Pattern.compile(SD_OTG_SHORT)

        if (requestCode == OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB) {
            if (resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
                if (isProperAndroidRoot(checkedDocumentPath, resultData.data!!)) {
                    if (resultData.dataString == mPref.OTGTreeUri || resultData.dataString == mPref.sdTreeUri) {
                        toast(R.string.wrong_root_selected)
                        return
                    }

                    val treeUri = resultData.data
                    storeAndroidTreeUri(checkedDocumentPath, treeUri.toString())

                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    applicationContext.contentResolver.takePersistableUriPermission(treeUri!!, takeFlags)
                    funAfterSAFPermission?.invoke(true)
                    funAfterSAFPermission = null
                } else {
                    toast(R.string.wrong_root_selected)
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        if (isRPlus()) {
                            putExtra(DocumentsContract.EXTRA_INITIAL_URI, createAndroidDataOrObbUri(checkedDocumentPath))
                        }
                        startActivityForResult(this, requestCode)
                    }
                }
            } else {
                funAfterSAFPermission?.invoke(false)
            }
        } else if (requestCode == OPEN_DOCUMENT_TREE_SD) {
            if (resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
                val isProperPartition = partition.isEmpty() || !sdOtgPattern.matcher(partition).matches() || (sdOtgPattern.matcher(partition)
                    .matches() && resultData.dataString!!.contains(partition))
                if (isProperSDRootFolder(resultData.data!!) && isProperPartition) {
                    if (resultData.dataString == mPref.OTGTreeUri) {
                        toast(R.string.sd_card_usb_same)
                        return
                    }

                    saveTreeUri(resultData)
                    funAfterSAFPermission?.invoke(true)
                    funAfterSAFPermission = null
                } else {
                    toast(R.string.wrong_root_selected)
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    startActivityForResult(intent, requestCode)
                }
            } else {
                funAfterSAFPermission?.invoke(false)
            }
        } else if (requestCode == OPEN_DOCUMENT_TREE_OTG) {
            if (resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
                val isProperPartition = partition.isEmpty() || !sdOtgPattern.matcher(partition).matches() || (sdOtgPattern.matcher(partition)
                    .matches() && resultData.dataString!!.contains(partition))
                if (isProperOTGRootFolder(resultData.data!!) && isProperPartition) {
                    if (resultData.dataString == mPref.sdTreeUri) {
                        funAfterSAFPermission?.invoke(false)
                        toast(R.string.sd_card_usb_same)
                        return
                    }
                    mPref.OTGTreeUri = resultData.dataString!!
                    mPref.OTGPartition = mPref.OTGTreeUri.removeSuffix("%3A").substringAfterLast('/').trimEnd('/')
                    updateOTGPathFromPartition()

                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    applicationContext.contentResolver.takePersistableUriPermission(resultData.data!!, takeFlags)

                    funAfterSAFPermission?.invoke(true)
                    funAfterSAFPermission = null
                } else {
                    toast(R.string.wrong_root_selected_usb)
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    startActivityForResult(intent, requestCode)
                }
            } else {
                funAfterSAFPermission?.invoke(false)
            }
        } else if (requestCode == SELECT_EXPORT_SETTINGS_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportSettingsTo(outputStream, configItemsToExport)
        } else if (requestCode == DELETE_FILE_SDK_30_HANDLER) {
            funAfterDelete30File?.invoke(resultCode == Activity.RESULT_OK)
        } else if (requestCode == RECOVERABLE_SECURITY_HANDLER) {
            funRecoverableSecurity?.invoke(resultCode == Activity.RESULT_OK)
            funRecoverableSecurity = null
        }
    }

    private fun saveTreeUri(resultData: Intent) {
        val treeUri = resultData.data
        mPref.sdTreeUri = treeUri.toString()

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(treeUri!!, takeFlags)
    }

    private fun isProperSDRootFolder(uri: Uri) = isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)
    private fun isProperSDFolder(uri: Uri) = isExternalStorageDocument(uri) && !isInternalStorage(uri)

    private fun isProperOTGRootFolder(uri: Uri) = isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)
    private fun isProperOTGFolder(uri: Uri) = isExternalStorageDocument(uri) && !isInternalStorage(uri)

    private fun isRootUri(uri: Uri) = uri.lastPathSegment?.endsWith(":") ?: false

    private fun isInternalStorage(uri: Uri) = isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri).contains("primary")
    private fun isAndroidDir(uri: Uri) = isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri).contains(":Android")
    private fun isInternalStorageAndroidDir(uri: Uri) = isInternalStorage(uri) && isAndroidDir(uri)
    private fun isOTGAndroidDir(uri: Uri) = isProperOTGFolder(uri) && isAndroidDir(uri)
    private fun isSDAndroidDir(uri: Uri) = isProperSDFolder(uri) && isAndroidDir(uri)
    private fun isExternalStorageDocument(uri: Uri) = EXTERNAL_STORAGE_PROVIDER_AUTHORITY == uri.authority

    private fun isProperAndroidRoot(path: String, uri: Uri): Boolean {
        return when {
            isPathOnOTG(path) -> isOTGAndroidDir(uri)
            isPathOnSD(path) -> isSDAndroidDir(uri)
            else -> isInternalStorageAndroidDir(uri)
        }
    }


    fun handleSAFDialog(path: String, callback: (success: Boolean) -> Unit): Boolean {
        return if (!packageName.startsWith("com.myapp")) {
            callback(true)
            false
        } else if (isShowingSAFDialog(path) || isShowingOTGDialog(path)) {
            funAfterSAFPermission = callback
            true
        } else {
            callback(true)
            false
        }
    }

    fun handleAndroidSAFDialog(path: String, callback: (success: Boolean) -> Unit): Boolean {
        return if (!packageName.startsWith("com.myapp")) {
            callback(true)
            false
        } else if (isShowingAndroidSAFDialog(path)) {
            funAfterSAFPermission = callback
            true
        } else {
            callback(true)
            false
        }
    }

    fun handleOTGPermission(callback: (success: Boolean) -> Unit) {
        if (mPref.OTGTreeUri.isNotEmpty()) {
            callback(true)
            return
        }

        funAfterSAFPermission = callback
        WritePermissionDialog(this, true) {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                try {
                    startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                    return@apply
                } catch (e: Exception) {
                    type = "*/*"
                }

                try {
                    startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                } catch (e: Exception) {
                    toast(R.string.unknown_error_occurred)
                }
            }
        }
    }

    @SuppressLint("NewApi")
    fun deleteSDK30Uris(uris: List<Uri>, callback: (success: Boolean) -> Unit) {
        if (isRPlus()) {
            funAfterDelete30File = callback
            try {
                val deleteRequest = MediaStore.createDeleteRequest(contentResolver, uris).intentSender
                startIntentSenderForResult(deleteRequest, DELETE_FILE_SDK_30_HANDLER, null, 0, 0, 0)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        } else {
            callback(false)
        }
    }


    fun checkConflicts(
        files: ArrayList<FileDIRModel>, destinationPath: String, index: Int, conflictResolutions: LinkedHashMap<String, Int>,
        callback: (resolutions: LinkedHashMap<String, Int>) -> Unit,
    ) {
        if (index == files.size) {
            callback(conflictResolutions)
            return
        }

        val file = files[index]
        val newFileDirItem = FileDIRModel("$destinationPath/${file.name}", file.name, file.isDirectory)
        if (getDoesFilePathExist(newFileDirItem.path)) {
            FileConflictDialog(this, newFileDirItem, files.size > 1) { resolution, applyForAll ->
                if (applyForAll) {
                    conflictResolutions.clear()
                    conflictResolutions[""] = resolution
                    checkConflicts(files, destinationPath, files.size, conflictResolutions, callback)
                } else {
                    conflictResolutions[newFileDirItem.path] = resolution
                    checkConflicts(files, destinationPath, index + 1, conflictResolutions, callback)
                }
            }
        } else {
            checkConflicts(files, destinationPath, index + 1, conflictResolutions, callback)
        }
    }

    fun handlePermission(permissionId: Int, callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (hasPermission(permissionId)) {
            callback(true)
        } else {
            isAskingPermissions = true
            actionOnPermission = callback
            ActivityCompat.requestPermissions(this, arrayOf(getPermissionString(permissionId)), GENERIC_PERM_HANDLER)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        isAskingPermissions = false
        if (requestCode == GENERIC_PERM_HANDLER && grantResults.isNotEmpty()) {
            actionOnPermission?.invoke(grantResults[0] == 0)
        }
    }

//    val copyMoveListener = object : CopyMoveCallback {
//        override fun copySucceeded(copyOnly: Boolean, copiedAll: Boolean, destinationPath: String, wasCopyingOneFileOnly: Boolean) {
//            if (copyOnly) {
//                toast(
//                    if (copiedAll) {
//                        if (wasCopyingOneFileOnly) {
//                            R.string.copying_success_one
//                        } else {
//                            R.string.copying_success
//                        }
//                    } else {
//                        R.string.copying_success_partial
//                    }
//                )
//            } else {
//                toast(
//                    if (copiedAll) {
//                        if (wasCopyingOneFileOnly) {
//                            R.string.moving_success_one
//                        } else {
//                            R.string.moving_success
//                        }
//                    } else {
//                        R.string.moving_success_partial
//                    }
//                )
//            }
//
//            copyMoveCallback?.invoke(destinationPath)
//            copyMoveCallback = null
//        }
//
//        override fun copyFailed() {
//            toast(R.string.copy_move_failed)
//            copyMoveCallback = null
//        }
//    }

    private fun exportSettingsTo(outputStream: OutputStream?, configItems: LinkedHashMap<String, Any>) {
        if (outputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        ensureBackgroundThread {
            outputStream.bufferedWriter().use { out ->
                for ((key, value) in configItems) {
                    out.writeLn("$key=$value")
                }
            }

            toast(R.string.settings_exported_successfully)
        }
    }


    open fun dimBackgroundPopWindow(context: Context, popupWindow: PopupWindow) {
        val view = popupWindow.contentView.parent as View
        val wm = context.getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutParams = view.layoutParams as WindowManager.LayoutParams
        layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        layoutParams.dimAmount = 0.3f
        wm.updateViewLayout(view, layoutParams)
    }

    open fun dismissPopup(popupWindow: PopupWindow) {
        if (popupWindow.isShowing) {
            popupWindow.dismiss()
        }
    }

    fun showExitDialog(
        mContext: Context,
    ) {
        val builder = BottomSheetDialog(mContext)
        builder.setCancelable(true)
        val dialogBinding = DialogExitBinding.inflate(LayoutInflater.from(mContext))
        builder.setContentView(dialogBinding.root)
        builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        builder.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        AdsManager.showMediumRectangleBannerAds(dialogBinding.mNativeContainer, dialogBinding.llNativeShimmer, mContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dialogBinding.btnNotNow.setGradientColors(getColor(R.color.blue), getColor(R.color.purple))
        }
        dialogBinding.btnNotNow.setOnClickListener {
            builder.dismiss()
        }

        dialogBinding.btnYes.setOnClickListener {
            finish()
        }
        builder.show()
    }

    fun showInterstitialAdPerDayOnDrawerItemClick(onItemClick: () -> Unit) {
        val lastAdDisplayTime = getSharedPrefs().getLong(LAST_AD_DISPLAY_DRAWER_ITEM_CLICK, 0L)
        val currentTime = Calendar.getInstance().timeInMillis
        if (currentTime - lastAdDisplayTime >= ONE_DAY_IN_MILLIS) {
            showDialog()
            AdsManager.loadInterstitialAds(this, object : AdsDismissCallback {
                override fun onAdDismiss() {
                    dismissDialog()
                    onItemClick.invoke()
                }
            })
            getSharedPrefs().edit().putLong(LAST_AD_DISPLAY_DRAWER_ITEM_CLICK, currentTime).apply()
        } else {
            dismissDialog()
            onItemClick.invoke()
        }
    }

    var ad: CircularProgressDialog? = null

    fun showDialog() {
        runOnUiThread {
            try {
                Log.d("TAG_DIALOG", "showDialog: $loadingDialog")
                loadingDialog?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

     fun initLoadingDialog(activity: Context) {
        loadingDialog = Dialog(activity)
        val binding: LayoutLoadingBinding =
            LayoutLoadingBinding.inflate(LayoutInflater.from(activity))
        loadingDialog?.setContentView(
            binding.root
        )
        loadingDialog?.setCancelable(
            false
        )

        val height = activity.resources.displayMetrics.heightPixels
        val width = activity.resources.displayMetrics.widthPixels
        Objects.requireNonNull<Window>(loadingDialog?.window)
            .setLayout(width, height)
        Objects.requireNonNull<Window>(loadingDialog?.window)
            .setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )

    }

    fun dismissDialog() {
        runOnUiThread {
            try {
                loadingDialog?.dismiss()
            } catch (e: Exception) {
                Log.d("TAG_ERROR", "dismissDialog: ${e.localizedMessage}")
            }
        }
    }

    override fun onBackPressed() {
        if (SHOW_INTERTAIL_ADD_PER_COUNT % 5 == 0) {
            showDialog()
            AdsManager.loadInterstitialAds(this, object : AdsDismissCallback {
                override fun onAdDismiss() {
                    dismissDialog()
                    SHOW_INTERTAIL_ADD_PER_COUNT++
                    finish()
                }
            })
        } else {
            dismissDialog()
            SHOW_INTERTAIL_ADD_PER_COUNT++
            super.onBackPressed()
        }
    }

    fun removeCommonItems(list1: MutableList<ConversationSmsModel>, list2: List<ArchivedModel>): ArrayList<ConversationSmsModel> {
        list1.removeAll { item1 ->
            list2.any { item2 ->
                // Customize this condition according to your object comparison logic
                item1.phoneNumber == item2.number // Assuming 'id' is a property used for comparison
            }
        }

        return list1 as ArrayList<ConversationSmsModel>
    }

    fun removeArchiveFromContacts(list1: MutableList<ContactsModel>, list2: List<ArchivedModel>): ArrayList<ContactsModel> {
        list1.removeAll { item1 ->
            list2.any { item2 ->
                // Customize this condition according to your object comparison logic
                item1.name.lowercase() == item2.name.lowercase() // Assuming 'id' is a property used for comparison
            }
        }

        return list1 as ArrayList<ContactsModel>
    }

    fun removeBlockFromContacts(list1: MutableList<ContactsModel>, list2: List<BlockContactModel>): ArrayList<ContactsModel> {
        list1.removeAll { item1 ->
            list2.any { item2 ->
                // Customize this condition according to your object comparison logic
                item1.name.lowercase() == item2.name.lowercase() // Assuming 'id' is a property used for comparison
            }
        }

        return list1 as ArrayList<ContactsModel>
    }

}
