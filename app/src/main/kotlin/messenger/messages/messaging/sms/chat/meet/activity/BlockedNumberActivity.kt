package messenger.messages.messaging.sms.chat.meet.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import androidx.core.view.isVisible
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.adapters.BlockedNumberAdapter
import messenger.messages.messaging.sms.chat.meet.dialogs.AddToBlockDialog
import messenger.messages.messaging.sms.chat.meet.dialogs.ExportBlockNumberDialog
import messenger.messages.messaging.sms.chat.meet.dialogs.FilePickerDialog
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.listners.RefreshingRecyclerListner
import messenger.messages.messaging.sms.chat.meet.utils.*
import messenger.messages.messaging.sms.chat.meet.utils.BlockedNumbersExporterUtils.ExportResult
import kotlinx.coroutines.*
import messenger.messages.messaging.sms.chat.meet.databinding.ActivityBlockedNumberBinding
import messenger.messages.messaging.sms.chat.meet.model.BlockContactModel
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

class BlockedNumberActivity : BaseHomeActivity(), RefreshingRecyclerListner {
    private lateinit var binding: ActivityBlockedNumberBinding
    private val PICK_IMPORT_SOURCE_INTENT = 11
    private val PICK_EXPORT_FILE_INTENT = 21
    private var isNumberSelected = false
    override fun getAppIconIDs() = intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: ArrayList()
    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundResource(R.drawable.bg_gradient)
        binding = ActivityBlockedNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.appTopToolbar.txtHeading.text = getString(R.string.app_block_number)
        binding.appTopToolbar.imgBack.setOnClickListener {
            onBackPressed()
        }

        if (!PrefClass.isProUser){
        showBannerAds(findViewById(R.id.mBannerAdsContainer))
        }else{
            findViewById<ViewGroup>(R.id.mBannerAdsContainer)?.visibility = View.GONE
        }
        updateBlockedNumbers()
        updatePlaceholderTexts()

    }

    @SuppressLint("InlinedApi")
    protected fun defaultDialer() {
        if (isQPlus()) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
            }
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName).apply {
                try {
                    startActivityForResult(this, REQUEST_CODE_SET_DEFAULT_DIALER)
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.no_app_found)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.opt_menu_block_no, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuAdd -> addOrEditBlockedNumber()
            R.id.menuMore -> showMoreDialog()

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun refreshItems() {
        updateBlockedNumbers()
    }

    private fun updatePlaceholderTexts() {
        binding.tvNoData1.text = getString(if (isDefaultDialer()) R.string.not_blocking_anyone else R.string.must_make_default_dialer)
    }

    private fun updateBlockedNumbers() {
        ensureBackgroundThread {
            val blockedNumbers = getBlockedNumbers1()
            runOnUiThread {
                BlockedNumberAdapter(this, blockedNumbers, this, binding.recyclerViewBlockNumber, itemClick = {

                }, itemLongClick = {
                    Log.d("TAG_BLOCK", "updateBlockedNumbers: $it $isNumberSelected ${blockedNumbers.size}")
                    if (it == 0 && isNumberSelected) {
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(300)
                            withContext(Dispatchers.Main) {
                                isNumberSelected = false
                                binding.appTopToolbar.toolbar.isVisible = true
                            }
                        }
                    } else {
                        isNumberSelected = true
                        binding.appTopToolbar.toolbar.isVisible = false
                    }
                }).apply {
                    binding.recyclerViewBlockNumber.adapter = this
                }

                binding.tvNoData1.beVisibleIf(blockedNumbers.isEmpty())
                binding.ivThumbNodata.beVisibleIf(blockedNumbers.isEmpty())
            }
        }
    }

    private fun addOrEditBlockedNumber(currentNumber: BlockContactModel? = null) {
        AddToBlockDialog(this, currentNumber) {
            updateBlockedNumbers()
        }
    }

    private fun tryImportBlockedNumbers() {
        if (isQPlus()) {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
            }
        } else {
            handlePermission(PERMISSION_READ_STORAGE) {
                if (it) {
                    pickFileToImportBlockedNumbers()
                }
            }
        }
    }

    private fun tryImportBlockedNumbersFromFile(uri: Uri) {
        when (uri.scheme) {
            "file" -> importBlockedNumbers(uri.path!!)
            "content" -> {
                val tempFile = getTempFile("blocked", "blocked_numbers.txt")
                if (tempFile == null) {
                    toast(R.string.unknown_error_occurred)
                    return
                }

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val out = FileOutputStream(tempFile)
                    inputStream!!.copyTo(out)
                    importBlockedNumbers(tempFile.absolutePath)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }

            else -> toast(R.string.invalid_file_format)
        }
    }

    private fun pickFileToImportBlockedNumbers() {
        FilePickerDialog(this) {
            importBlockedNumbers(it)
        }
    }

    private fun importBlockedNumbers(path: String) {
        ensureBackgroundThread {
            val result = BlockedNumbersImporterUtils(this).importBlockedNumbers(path)
            toast(
                when (result) {
                    BlockedNumbersImporterUtils.ImportResult.IMPORT_OK -> R.string.importing_successful
                    BlockedNumbersImporterUtils.ImportResult.IMPORT_FAIL -> R.string.no_items_found
                }
            )
            updateBlockedNumbers()
        }
    }

    private fun tryExportBlockedNumbers() {
        if (isQPlus()) {
            ExportBlockNumberDialog(this, mPref.lastBlockedNumbersExportPath, true) { file ->
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    addCategory(Intent.CATEGORY_OPENABLE)
                    startActivityForResult(this, PICK_EXPORT_FILE_INTENT)
                }
            }
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) {
                if (it) {
                    ExportBlockNumberDialog(this, mPref.lastBlockedNumbersExportPath, false) { file ->
                        getFileOutputStream(file.toFileDirItem(this), true) { out ->
                            exportBlockedNumbersTo(out)
                        }
                    }
                }
            }
        }
    }

    private fun exportBlockedNumbersTo(outputStream: OutputStream?) {
        ensureBackgroundThread {
            val blockedNumbers = getBlockedNumbers()
            if (blockedNumbers.isEmpty()) {
                toast(R.string.no_entries_for_exporting)
            } else {
                BlockedNumbersExporterUtils().exportBlockedNumbers(blockedNumbers, outputStream) {
                    toast(
                        when (it) {
                            ExportResult.EXPORT_OK -> R.string.exporting_successful
                            ExportResult.EXPORT_FAIL -> R.string.exporting_failed
                        }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER && isDefaultDialer()) {
            updatePlaceholderTexts()
            updateBlockedNumbers()
        } else if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            tryImportBlockedNumbersFromFile(resultData.data!!)
        } else if (requestCode == PICK_EXPORT_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportBlockedNumbersTo(outputStream)
        }
    }

    private fun showMoreDialog() {
        val view: View = layoutInflater.inflate(R.layout.dialog_import_export_number, null)
        val popupMore = PopupWindow(
            view, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT, true
        )
        popupMore.animationStyle = android.R.style.Animation_Dialog
        popupMore.showAtLocation(view, Gravity.TOP or Gravity.END, 0, 180)
        dimBackgroundPopWindow(this, popupMore)

        view.findViewById<View>(R.id.llImportNumber).setOnClickListener { v: View? ->
            dismissPopup(popupMore)
            tryImportBlockedNumbers()
        }
        view.findViewById<View>(R.id.llExportNumber).setOnClickListener { v: View? ->
            dismissPopup(popupMore)
            tryExportBlockedNumbers()
        }
    }
}
