package messenger.messages.messaging.sms.chat.meet.extensions

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Long.formatDateOrTime(context: Context, hideTimeAtOtherDays: Boolean, showYearEvenIfCurrent: Boolean): String {
    val cal = Calendar.getInstance(Locale.US)
    cal.timeInMillis = this

    return if (DateUtils.isToday(this)) {
        DateFormat.format(context.getTimeFormat(), cal).toString()
    } else {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())

        // Create a Date object from milliseconds
        val date = Date(this)

        // Format the Date object and return the formatted string
        return sdf.format(date)

    }

}

fun Long.formatFullDateOrTime(context: Context, hideTimeAtOtherDays: Boolean, showYearEvenIfCurrent: Boolean): String {
    val cal = Calendar.getInstance(Locale.US)
    cal.timeInMillis = this

    val sdf = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())

    // Create a Date object from milliseconds
    val date = Date(this)

    // Format the Date object and return the formatted string
    return sdf.format(date)


}

