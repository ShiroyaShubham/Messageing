package messenger.messages.messaging.sms.chat.meet.logger

import android.util.Log
import messenger.messages.messaging.sms.chat.meet.BuildConfig

class AppLogger : Logger {

    private val APP_TAG = "AdsDemo : "

    override fun v(classInstance: Class<out Any?>?, msg: String?) {

        if (classInstance != null && BuildConfig.DEBUG) {
            Log.v(
                APP_TAG + classInstance.simpleName, msg!!
            )
        }
    }

    override fun i(classInstance: Class<out Any?>?, msg: String?) {
        if (classInstance != null && BuildConfig.DEBUG) {
            Log.i(
                APP_TAG + classInstance.simpleName, msg!!
            )
        }
    }

    override fun w(classInstance: Class<out Any?>?, msg: String?) {
        if (classInstance != null && BuildConfig.DEBUG) {
            Log.w(
                APP_TAG + classInstance.simpleName, msg!!
            )
        }
    }

    override fun d(classInstance: Class<out Any?>?, msg: String?) {
        if (classInstance != null && BuildConfig.DEBUG) {
            Log.d(
                APP_TAG + classInstance.simpleName, msg!!
            )
        }
    }

    override fun e(classInstance: Class<out Any?>?, msg: String?) {
        if (classInstance != null && BuildConfig.DEBUG) {
            Log.e(
                APP_TAG + classInstance.simpleName, msg!!
            )
        }
    }
}
