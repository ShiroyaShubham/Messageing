package messenger.messages.messaging.sms.chat.meet.fragment

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import messenger.messages.messaging.sms.chat.meet.activity.LanguageActivity
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.utils.*


class PermissionFragment : BaseFragment() {

    private val MAKE_DEFAULT_APP_REQUEST = 1
    private var btnContinue: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_permission, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onBackPress()
        btnContinue = view.findViewById(R.id.btnContinue)
        val imgIntro: ImageView = view.findViewById(R.id.imgIntro)
        val imgIntroDarkMode: ImageView = view.findViewById(R.id.imgIntroDarkMode)
        if(Utils.isDarkMode(requireContext())){
             imgIntro.isInvisible = true
            imgIntroDarkMode.isVisible = true
        }else{
            imgIntroDarkMode.isInvisible = true
            imgIntro.isVisible = true
        }
        btnContinue?.setOnClickListener {
            getPermission()
        }

    }


    private fun getPermission() {
        if (isQPlus()) {
            val roleManager = requireContext().getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    Log.e("Event: ", "askPermissions isQPlus")
                    askPermissions()
                } else {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                }
            } else {
                mActivity?.finish()
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(requireContext()) == mActivity!!.packageName) {
                Log.e("Event: ", "askPermissions isQPlus else")
                askPermissions()
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, mActivity!!.packageName)
                startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
            }
        }
    }


    // while SEND_SMS and READ_SMS permissions are mandatory, READ_CONTACTS is optional. If we don't have it, we just won't be able to show the contact name in some cases
    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            checkNotificationPermission()
                        }
                    } else {
                        showPermissionDialog()
                    }
                }
            } else {
                showPermissionDialog()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gotoNotificationScreen()
        } else {
            gotoHome()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!it) {
                Utility.showSnackBar(
                    requireActivity(),
                    getString(R.string.txt_please_grant_notification_permission_from_app_settings),
                    requireActivity().findViewById<View>(android.R.id.content).rootView,
                )
            } else {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

    private fun gotoNotificationScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun gotoHome() {
        startActivity(Intent(mActivity, LanguageActivity::class.java))
        mActivity?.finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == MAKE_DEFAULT_APP_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Log.e("Event: ", "askPermissions RESULT_OK")
                askPermissions()
            } else {
                showPermissionDialog()
            }
        }
    }

    private fun showPermissionDialog() {
        val mDialog = Dialog(mActivity!!)
        mDialog.setContentView(R.layout.dialog_permission)
        mDialog.setCanceledOnTouchOutside(false)
        mDialog.setCancelable(false)
        mDialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mDialog.window!!.attributes.windowAnimations = android.R.style.Animation_Dialog
        mDialog.show()

        (mDialog.findViewById<View>(R.id.tvSetAsDefault) as TextView).setOnClickListener {
            if (!mActivity?.isFinishing!! && mDialog.isShowing) {
                mDialog.dismiss()
            }
            getPermission()
        }
    }

    private fun onBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(requireContext(), LanguageActivity::class.java))
                requireActivity().finish()
            }
        })
    }


}
