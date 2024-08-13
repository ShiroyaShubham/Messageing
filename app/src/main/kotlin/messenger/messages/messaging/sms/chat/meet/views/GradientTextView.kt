package messenger.messages.messaging.sms.chat.meet.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class GradientTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var startColor = 0
    private var endColor = 0

    fun setGradientColors(startColor: Int, endColor: Int) {
        this.startColor = startColor
        this.endColor = endColor
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val textPaint: Paint = paint
        val width = width.toFloat()
        val height = height.toFloat()

        val shader = LinearGradient(
            0f, 0f, width, height,
            startColor, endColor,
            Shader.TileMode.CLAMP
        )

        textPaint.shader = shader

        super.onDraw(canvas)
    }
}

