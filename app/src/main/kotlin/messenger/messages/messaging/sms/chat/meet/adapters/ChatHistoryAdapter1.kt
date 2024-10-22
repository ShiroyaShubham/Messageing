package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseActivity
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.databinding.ItemAdvanceNativeBannerBinding
import messenger.messages.messaging.sms.chat.meet.databinding.ListRawChatHistoryBinding
import messenger.messages.messaging.sms.chat.meet.extensions.beVisibleIf
import messenger.messages.messaging.sms.chat.meet.extensions.formatDateOrTime
import messenger.messages.messaging.sms.chat.meet.extensions.getTextSize
import messenger.messages.messaging.sms.chat.meet.model.ConversationSmsModel
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass
import messenger.messages.messaging.sms.chat.meet.utils.Utility
import messenger.messages.messaging.sms.chat.meet.utils.config
import messenger.messages.messaging.sms.chat.meet.utils.getAllDrafts
import messenger.messages.messaging.sms.chat.meet.utils.getContactNameFromPhoneNumber

class ChatHistoryAdapter1(val mActivity: BaseHomeActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var mConversations: ArrayList<ConversationSmsModel> = arrayListOf()
    private var drafts = HashMap<Long, String?>()
    private var fontSize = mActivity.getTextSize()

    init {
        fetchDrafts(drafts)
    }

    var IS_AD_SHOWN = false
    private val BANNER_AD = 1
    private val CONTENT = 2

    var isShowSelection: Boolean = false

    lateinit var itemClickListener: ItemClickListener<ConversationSmsModel>
    lateinit var itemClickListenerSelect: ItemClickListener1<ConversationSmsModel>

    lateinit var itemLongClickListener: ItemClickListener<ConversationSmsModel>

    inner class ViewHolder(private val bind: ListRawChatHistoryBinding) : RecyclerView.ViewHolder(bind.root) {
        fun bindData(position: Int) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val conversation = mConversations[position]
                    val smsDraft = drafts[conversation.threadId]

                    // Check if the title is a phone number (no contact name yet)
                    if (Utility.isNumber(conversation.title)) {
                        // Try to get the contact name from the number
                        val contactName = getContactName(conversation.title, mActivity)

                        if (!contactName.isNullOrEmpty()) {
                            // If contact name is found, update the conversation's title with the contact name
                            conversation.title = contactName
                            notifyItemChanged(position) // Update the specific item in RecyclerView
                        }
                    }
                    Log.e("TAG", "bindData:>>>> "+conversation.title )

                    bind.tvConversationTitle.apply {
                        text = conversation.title
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
                    }

                    bind.tvConversationDesc.apply {
                        text = smsDraft ?: conversation.snippet
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
                    }

                    bind.tvDate.apply {
                        text = conversation.date.formatDateOrTime(context, true, false)
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
                    }

                    bind.tvDraftIndicator.beVisibleIf(smsDraft != null)
                    bind.ivPinThumb.beVisibleIf(mActivity.config.pinnedConversations.contains(conversation.threadId.toString()))
                    bind.tvConversationTitle.text = conversation.title
                    bind.tvConversationDesc.text = smsDraft ?: conversation.snippet
                    bind.tvDate.text = conversation.date.formatDateOrTime(mActivity, true, false)
                    if (conversation.read) {
                        bind.tvDate.setGradientColors(mActivity.getColor(R.color.text_grey), mActivity.getColor(R.color.text_grey))
                        bind.imgUnread.isVisible = false
                    } else {
                        bind.tvDate.setGradientColors(mActivity.getColor(R.color.blue), mActivity.getColor(R.color.purple))
                        bind.imgUnread.isVisible = true
                    }

                    if (conversation.title.isNotEmpty()) {
                        val firstChar: Char = conversation.title[0]
                        if (Utils.isAlphabet(firstChar)) {
                            bind.ivThumb.isVisible = false
                            bind.txtFirstLetter.isVisible = true
                            bind.txtFirstLetter.text = firstChar.toString()
                        } else {
                            bind.ivThumb.isVisible = true
                            bind.txtFirstLetter.isVisible = false
                        }
                    }
                    bind.rlMain.setOnClickListener {
                        itemClickListenerSelect(absoluteAdapterPosition, conversation)
                    }

                } catch (e: Exception) {
                    Log.d("TAG_ERROR", "bindData: ${e.message}")
                }

            }

        }

    }

    inner class ViewHolderAds(private val bind: ItemAdvanceNativeBannerBinding) : RecyclerView.ViewHolder(bind.root) {
        fun bindData(position: Int) {
            val conversation = mConversations[position]
            val smsDraft = drafts[conversation.threadId]

            Log.e("TAG", "bindData: inside adapter $position")

            if (!IS_AD_SHOWN) {
                if (mActivity is BaseActivity) {
                    IS_AD_SHOWN = true
                    itemView.apply {
                        if (!PrefClass.isProUser) {
                            findViewById<ViewGroup>(R.id.mMediumAdsContainer)?.visibility = View.VISIBLE
                            mActivity.showSmallNativeAds(findViewById(R.id.mMediumAdsContainer))
                        }else{
                            findViewById<ViewGroup>(R.id.mMediumAdsContainer)?.visibility = View.GONE
                        }
//                        messenger.messages.messaging.sms.chat.meet.ads.AdsManager.showNativeBannerAds(findViewById(R.id.mNativeAds), mActivity)
                    }
                }

            }

        }
    }

    fun getIsShowSelection(): Boolean {
        return isShowSelection
    }

    fun setIsShowSelection(isShowSelection: Boolean) {
        this.isShowSelection = isShowSelection
    }

    fun getFileListData(): ArrayList<ConversationSmsModel> {
        return mConversations
    }

//    fun getFileListDataSelected(): ArrayList<ConversationSmsModel> {
//        return mConversations.filter { it.isSelected } as ArrayList<ConversationSmsModel>
//    }

//    fun setFileListDataUnselected() {
//
//        for (i in mConversations.indices) {
//            if (mConversations[i].isSelected) {
//                mConversations[i].isSelected = false
//                notifyItemChanged(i)
//            }
//        }
//    }

//    fun pinConversation(pin: Boolean) {
//        val mConversations = getSelectedItemsPin()
//        if (mConversations.isEmpty()) {
//            return
//        }
//
//        if (pin) {
//            mActivity.config.addPinnedConversations(mConversations)
//        } else {
//            mActivity.config.removePinnedConversations(mConversations)
//        }
//
//        mActivity.runOnUiThread {
//            Log.e("Event: ", "pinConversation")
//            refreshMessages()
////            finishActMode()
//        }
//    }

//    fun removeSelectedItems() {
//        mConversations.filter { it.isSelected }.forEach { mConversations.remove(it) }
//    }

//    fun readSelectedItems(isRead: Boolean) {
//        mConversations.filter { it.isSelected }.forEach { it.read = isRead }
//    }

//    fun removeSelection() {
//        val filterItems = mConversations.filter { it.isSelected }//.forEach { it.isSelected = false }
//        /* mConversations.withIndex().forEach {
//             mConversations[it.index].isSelected = false
//         }*/
//        for (item in filterItems) {
//            val pos = mConversations.indexOf(item)
//            mConversations[pos].isSelected = false
//            println("<><><><><><> Selection $pos")
//            notifyItemChanged(pos)
//        }
//    }

//    private fun getSelectedItemsPin() = mConversations.filter { it.isSelected } as ArrayList<ConversationSmsModel>


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            BANNER_AD -> {
                ViewHolderAds(ItemAdvanceNativeBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            CONTENT -> {
                ViewHolder(ListRawChatHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            else -> {
                ViewHolder(ListRawChatHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }

//        return ViewHolder(ListRawChatHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolderAds) {

            holder.bindData(position)

        } else if (holder is ViewHolder) {
            holder.bindData(position)
        }
    }

    /*override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(position)
    }*/

    override fun getItemCount(): Int {
        Log.d("TAG_SIZE", "getItemCount: ${mConversations.size}")
        return mConversations.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (mConversations[position].date == 1.toLong()) {
            BANNER_AD
        } else {
            CONTENT
        }
    }

    private fun fetchDrafts(drafts: HashMap<Long, String?>) {
        drafts.clear()
        for ((threadId, draft) in mActivity.getAllDrafts()) {
            drafts[threadId] = draft
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateFontSize() {
        if (fontSize.equals(mActivity.getTextSize())) {
            return
        }
        fontSize = mActivity.getTextSize()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDrafts() {
        val newDrafts = HashMap<Long, String?>()
        fetchDrafts(newDrafts)
        if (drafts.hashCode() != newDrafts.hashCode()) {
            drafts = newDrafts
            notifyDataSetChanged()
        }
    }

    fun setData(newArrayList: ArrayList<ConversationSmsModel>) {
        val filteredList = newArrayList.distinctBy { it.threadId } // Avoid duplicate threadIds
        val petDiffUtilCallback = PetDiffUtilCallback(mConversations, ArrayList(filteredList))
        val diffResult = DiffUtil.calculateDiff(petDiffUtilCallback)
        mConversations.clear()
        mConversations.addAll(newArrayList)
        diffResult.dispatchUpdatesTo(this)
    }

    class PetDiffUtilCallback(
        private val oldList: ArrayList<ConversationSmsModel>,
        private val newList: ArrayList<ConversationSmsModel>
    ) :
        DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            if (newList.isNotEmpty() && oldList.isNotEmpty()) newList[newItemPosition].threadId == oldList[oldItemPosition].threadId else true


        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            if (newList.isNotEmpty() && oldList.isNotEmpty()) newList[newItemPosition] == oldList[oldItemPosition] else true
    }

    // Function to get the contact name from a phone number
    fun getContactName(phoneNumber: String, context: Context): String? {
        var contactName: String? = null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                contactName = it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return contactName
    }

}


typealias ItemClickListener<T> = (position: Int, data: T, isLeft: Boolean) -> Unit
typealias ItemClickListener1<T> = (position: Int, data: T) -> Unit

