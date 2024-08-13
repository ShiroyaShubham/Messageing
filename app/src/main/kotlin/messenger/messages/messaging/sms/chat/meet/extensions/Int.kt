package messenger.messages.messaging.sms.chat.meet.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.format.Time
import messenger.messages.messaging.sms.chat.meet.utils.DARK_GREY
import java.text.SimpleDateFormat
import java.util.*

fun Int.getContrastColor(): Int {
    val y = (299 * Color.red(this) + 587 * Color.green(this) + 114 * Color.blue(this)) / 1000
    return if (y >= 149 && this != Color.BLACK) DARK_GREY else Color.WHITE
}

fun Int.toHex() = String.format("#%06X", 0xFFFFFF and this).toUpperCase()

fun Int.adjustAlpha(factor: Float): Int {
    val alpha = Math.round(Color.alpha(this) * factor)
    val red = Color.red(this)
    val green = Color.green(this)
    val blue = Color.blue(this)
    return Color.argb(alpha, red, green, blue)
}

fun Int.getFormattedDuration(forceShowHours: Boolean = false): String {
    val sb = StringBuilder(8)
    val hours = this / 3600
    val minutes = this % 3600 / 60
    val seconds = this % 60

    if (this >= 3600) {
        sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
    } else if (forceShowHours) {
        sb.append("0:")
    }

    sb.append(String.format(Locale.getDefault(), "%02d", minutes))
    sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))
    return sb.toString()
}

// if the given date is today, we show only the time. Else we show the date and optionally the time too
fun Int.formatDateOrTime(context: Context, hideTimeAtOtherDays: Boolean, showYearEvenIfCurrent: Boolean): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this * 1000L

    return if (DateUtils.isToday(this * 1000L)) {
        DateFormat.format(context.getTimeFormat(), cal).toString()
    } else {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())

        // Create a Date object from milliseconds
        val date = Date(this * 1000L)

        // Format the Date object and return the formatted string
        return sdf.format(date)

    }
}

fun Int.formatDateOrTimeForMessage(context: Context, hideTimeAtOtherDays: Boolean, showYearEvenIfCurrent: Boolean): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this * 1000L

    return if (DateUtils.isToday(this * 1000L)) {
        DateFormat.format(context.getTimeFormat(), cal).toString()
    } else {
        var format = context.mPref.dateFormat
        if (!showYearEvenIfCurrent && isThisYear()) {
            format = format.replace("y", "").trim().trim('-').trim('.').trim('/')
        }

        if (!hideTimeAtOtherDays) {
            format += ", ${context.getTimeFormat()}"
        }

        DateFormat.format(format, cal).toString()
    }
}

fun Int.isThisYear(): Boolean {
    val time = Time()
    time.set(this * 1000L)

    val thenYear = time.year
    time.set(System.currentTimeMillis())

    return (thenYear == time.year)
}

// TODO: how to do "bits & ~bit" in kotlin?
fun Int.removeBit(bit: Int) = addBit(bit) - bit

fun Int.addBit(bit: Int) = this or bit

fun Int.darkenColor(): Int {
    if (this == Color.WHITE || this == Color.BLACK) {
        return this
    }

    val DARK_FACTOR = 8
    var hsv = FloatArray(3)
    Color.colorToHSV(this, hsv)
    val hsl = hsv2hsl(hsv)
    hsl[2] -= DARK_FACTOR / 100f
    if (hsl[2] < 0)
        hsl[2] = 0f
    hsv = hsl2hsv(hsl)
    return Color.HSVToColor(hsv)
}

private fun hsl2hsv(hsl: FloatArray): FloatArray {
    val hue = hsl[0]
    var sat = hsl[1]
    val light = hsl[2]
    sat *= if (light < .5) light else 1 - light
    return floatArrayOf(hue, 2f * sat / (light + sat), light + sat)
}

private fun hsv2hsl(hsv: FloatArray): FloatArray {
    val hue = hsv[0]
    val sat = hsv[1]
    val value = hsv[2]

    val newHue = (2f - sat) * value
    var newSat = sat * value / if (newHue < 1f) newHue else 2f - newHue
    if (newSat > 1f)
        newSat = 1f

    return floatArrayOf(hue, newSat, newHue / 2f)
}

fun Int.ensureTwoDigits(): String {
    return if (toString().length == 1) {
        "0$this"
    } else {
        toString()
    }
}
fun Int.getColorStateList(): ColorStateList {
    val states = arrayOf(intArrayOf(android.R.attr.state_enabled),
        intArrayOf(-android.R.attr.state_enabled),
        intArrayOf(-android.R.attr.state_checked),
        intArrayOf(android.R.attr.state_pressed)
    )
    val colors = intArrayOf(this, this, this, this)
    return ColorStateList(states, colors)
}