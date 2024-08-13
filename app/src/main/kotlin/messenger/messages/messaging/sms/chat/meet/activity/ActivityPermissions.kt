package messenger.messages.messaging.sms.chat.meet.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import messenger.messages.messaging.sms.chat.meet.utils.SharedPrefrenceClass
import messenger.messages.messaging.sms.chat.meet.fragment.PermissionFragment
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.utils.IS_LOGIN
import messenger.messages.messaging.sms.chat.meet.utils.LANG_CODE
import messenger.messages.messaging.sms.chat.meet.R

class ActivityPermissions : AppCompatActivity() {


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.statusBarColor = resources.getColor(R.color.screen_background_color)
        setContentView(R.layout.activity_introduction)
        setLanguage(this)
        if(SharedPrefrenceClass.getInstance()?.getBoolean(IS_LOGIN,false) == true){
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.introductionContainer, PermissionFragment())
        transaction.addToBackStack(null)
        transaction.commit()

    }

    private fun setLanguage(context: Context) {
        Utils.setLocale(context, SharedPrefrenceClass.getInstance()?.getString(LANG_CODE,"en"))
    }

    override fun onBackPressed() {
        finish()
    }

}
