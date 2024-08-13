package messenger.messages.messaging.sms.chat.meet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "block_table", indices = [(Index(value = ["block_id"], unique = true))])
data class BlockContactModel(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "block_id") var blockID: Int = 0,
    @ColumnInfo(name = "threadID") var threadID: Long,
    @ColumnInfo(name = "number") var number: String,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "msg") var msg: String,
    @ColumnInfo(name = "dateTime") var dateTime: Long
)
