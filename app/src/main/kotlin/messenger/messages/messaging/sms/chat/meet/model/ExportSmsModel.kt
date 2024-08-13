package messenger.messages.messaging.sms.chat.meet.model

import com.google.gson.annotations.SerializedName

data class ExportSmsModel(
    @SerializedName("sms")
    val sms: List<SmsBackupModel>?,
    @SerializedName("mms")
    val mms: List<MMSBackupModel>?,
)
