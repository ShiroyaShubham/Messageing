package messenger.messages.messaging.sms.chat.meet.model

import android.content.ContentValues
import android.provider.Telephony
import androidx.core.content.contentValuesOf
import com.google.gson.annotations.SerializedName

data class MMSModel(
    @SerializedName("address")
    val address: String,
    @SerializedName("type")
    val type: Int,
    @SerializedName("charset")
    val charset: Int
) {

    fun toContentValues(): ContentValues {
        // msgId would be added at the point of insertion
        // because it may have changed
        return contentValuesOf(
            Telephony.Mms.Addr.ADDRESS to address,
            Telephony.Mms.Addr.TYPE to type,
            Telephony.Mms.Addr.CHARSET to charset,
        )
    }
}
