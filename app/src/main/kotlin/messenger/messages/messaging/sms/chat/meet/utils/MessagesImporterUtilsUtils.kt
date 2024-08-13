package messenger.messages.messaging.sms.chat.meet.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import messenger.messages.messaging.sms.chat.meet.extensions.showErrorToast
import messenger.messages.messaging.sms.chat.meet.utils.MessagesImporterUtilsUtils.ImportResult.*
import messenger.messages.messaging.sms.chat.meet.model.ExportSmsModel
import java.io.File

class MessagesImporterUtilsUtils(private val context: Context) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL, IMPORT_NOTHING_NEW
    }

    private val gson = Gson()
    private val messageWriter = WriterUtils(context)
    private val config = context.config
    private var messagesImported = 0
    private var messagesFailed = 0

    fun importMessages(path: String, onProgress: (total: Int, current: Int) -> Unit = { _, _ -> }, callback: (result: ImportResult) -> Unit) {
        ensureBackgroundThread {
            try {

                val inputStream = if (path.contains("/")) {
                    File(path).inputStream()
                } else {
                    context.assets.open(path)
                }

                inputStream.bufferedReader().use { reader ->
                    val json = reader.readText()
                    val type = object : TypeToken<List<ExportSmsModel>>() {}.type
                    val messages = gson.fromJson<List<ExportSmsModel>>(json, type)
                    val totalMessages = messages.flatMap { it.sms ?: emptyList() }.size + messages.flatMap { it.mms ?: emptyList() }.size
                    if (totalMessages <= 0) {
                        callback.invoke(IMPORT_NOTHING_NEW)
                        return@ensureBackgroundThread
                    }

                    onProgress.invoke(totalMessages, messagesImported)
                    for (message in messages) {
                        if (config.importSms) {
                            message.sms?.forEach { backup ->
                                messageWriter.writeSmsMessage(backup)
                                messagesImported++
                                onProgress.invoke(totalMessages, messagesImported)
                            }
                        }
//                        if (config.importMms) {
//                            message.mms?.forEach { backup ->
//                                messageWriter.writeMmsMessage(backup)
//                                messagesImported++
//                                onProgress.invoke(totalMessages, messagesImported)
//                            }
//                        }
                        Log.e("Event: ", " MessagesImporterUtilsUtils importMessages")
                        //refreshMessages()
                    }
                }
            } catch (e: Exception) {
                context.showErrorToast(e)
                messagesFailed++
            }

            callback.invoke(
                when {
                    messagesImported == 0 -> IMPORT_FAIL
                    messagesFailed > 0 -> IMPORT_PARTIAL
                    else -> IMPORT_OK
                }
            )
        }
    }
}
