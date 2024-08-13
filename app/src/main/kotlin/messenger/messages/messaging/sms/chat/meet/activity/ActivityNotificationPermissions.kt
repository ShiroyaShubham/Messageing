package messenger.messages.messaging.sms.chat.meet.activity

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.utils.Utility

class ActivityNotificationPermissions : AppCompatActivity() {

    var textView: TextView? = null
    var textView1: TextView? = null
    var tvSetAsDefault: TextView? = null
    var anim_out: Animation? = null
    var anim_in: Animation? = null
    var tvSkip: TextView? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_notification_permission)



        tvSkip = findViewById(R.id.tvSkip)
        tvSetAsDefault = findViewById(R.id.tvSetAsDefault)
        textView = findViewById(R.id.tvHead)
        textView1 = findViewById(R.id.tvStr)
        textView?.text = getString(R.string.text_header_notification)
        textView1?.text = getString(R.string.text_desc_notification)
        doAnimTitleSubTitle()

        tvSetAsDefault?.setOnClickListener {

        }
        tvSkip?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }



    private fun doAnimTitleSubTitle() {
        textView!!.clearAnimation()
        textView1!!.clearAnimation()
        tvSetAsDefault!!.clearAnimation()

        anim_out = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        anim_out?.repeatCount = Animation.ABSOLUTE
        anim_out?.duration = 1500

        textView!!.animation = anim_out
        textView1!!.animation = anim_out
        tvSetAsDefault!!.animation = anim_out

        anim_in = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        anim_in?.repeatCount = Animation.ABSOLUTE
        anim_in?.duration = 1500
        anim_in?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                textView!!.startAnimation(anim_out)
                textView1!!.startAnimation(anim_out)
                tvSetAsDefault!!.startAnimation(anim_out)

            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }





}
