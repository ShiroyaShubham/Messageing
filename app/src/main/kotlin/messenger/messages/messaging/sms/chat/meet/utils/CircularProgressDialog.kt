package messenger.messages.messaging.sms.chat.meet.utils
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.send_message.Utils


class CircularProgressDialog @SuppressLint("InflateParams") constructor(internal var context: Activity) :
    Thread() {

    private var pd: Dialog? = null

    val isShowing: Boolean
        get() {
            try {
                return pd!!.isShowing
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }

    init {
        pd = Dialog(context)
        pd!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        pd!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        pd!!.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT))
        pd!!.setContentView(
                context.layoutInflater.inflate(R.layout.custom_progress, null),
                ViewGroup.LayoutParams(
                    getDeviceWidth(context),
                    getDeviceHeight(context)
                ))
    }

    override fun run() {
        try {

            Looper.prepare()

            context.runOnUiThread { pd!!.show() }

            Looper.loop()

        } catch (t: Exception) {
            t.printStackTrace()
        }

    }

    fun show() {
        try {
            this.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Synchronized
    fun dismiss() {
        try {

            context.runOnUiThread {
                if (pd != null && pd!!.isShowing) {
                    pd!!.dismiss()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Synchronized
    fun setMessage() {

        try {
            if (pd != null) {

                context.runOnUiThread {
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {

        fun getInstant(context: Activity): CircularProgressDialog {
            return CircularProgressDialog(context)
        }

    }

    fun getDeviceWidth(context: Context): Int {
        try {
            val metrics = context.resources.displayMetrics
            return metrics.widthPixels
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 480
    }

    fun getDeviceHeight(context: Context): Int {
        try {
            val metrics = context.resources.displayMetrics
            return metrics.heightPixels
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 800
    }

}
