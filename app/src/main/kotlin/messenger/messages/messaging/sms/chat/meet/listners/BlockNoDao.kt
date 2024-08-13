package messenger.messages.messaging.sms.chat.meet.listners

import androidx.room.*
import messenger.messages.messaging.sms.chat.meet.model.BlockContactModel

@Dao
interface BlockNoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlockNo(archivedModel: BlockContactModel)

    @Delete
    fun deleteBlockNo(blockModel: BlockContactModel)

    @Query("DELETE FROM block_table WHERE block_id = :blockID")
    fun deleteBlockedID(blockID: Int)

    @Query("DELETE FROM block_table WHERE threadID = :threadId")
    fun deleteByThreadId(threadId: Long)


    @Query("SELECT * FROM block_table")
    fun getAllBlockNo(): List<BlockContactModel>

    @Query("SELECT * FROM block_table WHERE threadID= :id")
    fun getBlockNobyID(id: Int): BlockContactModel

}
