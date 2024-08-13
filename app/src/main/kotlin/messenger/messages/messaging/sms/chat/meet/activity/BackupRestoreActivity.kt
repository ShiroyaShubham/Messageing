package messenger.messages.messaging.sms.chat.meet.activity

import android.app.Dialog
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.adapters.ImportSmsAdapter
import messenger.messages.messaging.sms.chat.meet.ads.AdsManager
import messenger.messages.messaging.sms.chat.meet.dialogs.ExportDialog
import messenger.messages.messaging.sms.chat.meet.extensions.getFileOutputStream
import messenger.messages.messaging.sms.chat.meet.extensions.showErrorToast
import messenger.messages.messaging.sms.chat.meet.extensions.toFileDirItem
import messenger.messages.messaging.sms.chat.meet.extensions.toast
import messenger.messages.messaging.sms.chat.meet.utils.*
import messenger.messages.messaging.sms.chat.meet.views.GradientTextView
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityBackupRestoreBinding
import messenger.messages.messaging.sms.chat.meet.model.BackupImportModel
import messenger.messages.messaging.sms.chat.meet.model.RefreshWhileBackModel
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.OutputStream


class BackupRestoreActivity : BaseHomeActivity() {
    private lateinit var binding: ActivityBackupRestoreBinding
    protected val PERMISSIONS_REQUEST_EXPORT = 111
    protected val PERMISSIONS_REQUEST_IMPORT = 222
    private val smsExporter by lazy { MessagesExporterUtils(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLoadingDialog(this)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.appTopToolbar.txtHeading.text = getString(R.string.backup_and_restore)

        loadMediumBannerAd()
        setClicks()
        setLastBackupDate()
    }

    private fun setLastBackupDate() {
        if (TextUtils.isEmpty(config.lastExportDate)) {
            binding.tvBackupDate.text = getString(R.string.no_backup_available)
        } else {
            binding.tvBackupDate.text = config.lastExportDate
        }
    }

    private fun loadMediumBannerAd() {
        AdsManager.showMediumRectangleBannerAds(binding.mNativeContainer, binding.llNativeShimmer, this)
    }

    private fun setClicks() {
        binding.llRestore.setOnClickListener {
            if (PermissionUtils.isPermissionGranted_R_W(this)) {
                tryToImportMessages()
            } else {
                PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_IMPORT)
            }
        }
        binding.llBackupNow.setOnClickListener {
            if (PermissionUtils.isPermissionGranted_R_W(this)) {

                tryToExportMessages()
            } else {
                PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_EXPORT)
            }
        }
        binding.appTopToolbar.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0) {
            if (requestCode == PERMISSIONS_REQUEST_EXPORT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val readPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (readPermission) {
                        tryToExportMessages()
                    } else {
                        PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_EXPORT)
                    }
                } else {
                    val readPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (readPermission && writePermission) {
                        tryToExportMessages()
                    } else {
                        PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_EXPORT)
                    }
                }
            }
            if (requestCode == PERMISSIONS_REQUEST_IMPORT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val readPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (readPermission) {
                        tryToImportMessages()
                    } else {
                        PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_EXPORT)
                    }
                } else {
                    val readPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (readPermission && writePermission) {
                        tryToImportMessages()
                    } else {
                        PermissionUtils.takePermission_R_W(this, PERMISSIONS_REQUEST_EXPORT)
                    }
                }
            }
        }
    }

    //export
    private fun tryToExportMessages() {
        ExportDialog(this) { file ->
            getFileOutputStream(file.toFileDirItem(this), true) { outStream ->
                exportMessagesTo(outStream)
            }
        }
    }


    private fun exportMessagesTo(outputStream: OutputStream?) {
        showDialog()
        ensureBackgroundThread {
            smsExporter.exportMessages(outputStream) {
                when (it) {
                    MessagesExporterUtils.ExportResult.EXPORT_OK -> {
                        toast(getString(R.string.exporting_successful))
                        setLastBackupDate()
                    }

                    else -> {
//                        toast(getString(R.string.exporting_failed))
                    }
                }
                dismissDialog()

            }
        }
    }

    private fun clickEnable(enable: Boolean) {
        binding.llRestore.isEnabled = enable
        binding.llBackupNow.isEnabled = enable
    }


    //import Messages

    private fun tryToImportMessages() {
        val arrayList = getAllSavedFontsPath()

        if (arrayList.size > 0) {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.app_layout_import_sms_dialog)
            dialog.setCanceledOnTouchOutside(true)
            dialog.setCancelable(true)
            dialog.window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
            )
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.attributes.windowAnimations = android.R.style.Animation_Dialog

            val recyclerView = dialog.findViewById<View>(R.id.recyclerView) as RecyclerView
            recyclerView.layoutManager = LinearLayoutManager(this)

            val mAdapter = ImportSmsAdapter(this, arrayList, object : ImportSmsAdapter.onContainerClickListner {
                override fun onContainerClick(path: String) {
                    SMS_Dialog_Import(path)
                    dialog.dismiss()
                }
            })
            recyclerView.adapter = mAdapter
            dialog.show()
        } else {
            showErrorToast(getString(R.string.no_backup_available))
        }

    }

    private fun getAllSavedFontsPath(): ArrayList<String> {
        val directory: File = ContextWrapper(this).filesDir

        val arrayList = ArrayList<String>()
        val sb = StringBuilder()
        sb.append(directory.path)
        val file = File(sb.toString())
        if (file.isDirectory) {
            for (fileNext in File(sb.toString()).listFiles()!!) {
                try {
                    if (fileNext.isFile) {
                        val mFile = Uri.fromFile(File(fileNext.absolutePath))
                        val fileExt = MimeTypeMap.getFileExtensionFromUrl(mFile.toString())
                        val filename: String = fileNext.name.substring(fileNext.name.lastIndexOf("/") + 1)

                        if (fileExt.equals("json") && filename.contains("Messages")) {
                            arrayList.add(fileNext.absolutePath)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return arrayList
    }

    fun SMS_Dialog_Import(path: String) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_conformation)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(true)
        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.setLayout(
            width, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvExportCancel = dialog.findViewById<TextView>(R.id.tvAlertCancel)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvAlertTitle)
        val tvDescription = dialog.findViewById<TextView>(R.id.tvAlertDesc)
        val tvExportOk = dialog.findViewById<GradientTextView>(R.id.tvAlertOk)

        tvTitle.text = getString(R.string.import_messages)
        tvDescription.text = getString(R.string.are_you_sure_you_want_to_import_sms_)
        tvDescription.text = getString(R.string.are_you_sure_you_want_to_import_sms_)
        tvExportOk.text = getString(R.string.app_import)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tvExportOk.setGradientColors(getColor(R.color.blue), getColor(R.color.purple))
        }
        tvExportCancel.setOnClickListener {
            dialog.dismiss()
        }

        tvExportOk.setOnClickListener {
            dialog.dismiss()
            config.importSms = true
            showDialog()
            ensureBackgroundThread {
                MessagesImporterUtilsUtils(this).importMessages(path) {
                    handleParseResult(it)
                    dismissDialog()
                    config.appRunCount = 1
//                    refreshMessages()
                    EventBus.getDefault().post(BackupImportModel())
                }
            }
        }
        if (!dialog.isShowing) dialog.show()
    }

    private fun handleParseResult(result: MessagesImporterUtilsUtils.ImportResult) {
        toast(
            when (result) {
                MessagesImporterUtilsUtils.ImportResult.IMPORT_OK -> R.string.importing_successful
                MessagesImporterUtilsUtils.ImportResult.IMPORT_PARTIAL -> R.string.importing_some_entries_failed
                else -> R.string.no_items_found
            }
        )
    }

    //loading

}
