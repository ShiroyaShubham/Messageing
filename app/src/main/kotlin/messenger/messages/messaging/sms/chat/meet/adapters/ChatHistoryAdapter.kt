package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.dialogs.AlertDialogCustom
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.model.ConversationSmsModel
import messenger.messages.messaging.sms.chat.meet.utils.*
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.subscription.PrefClass
import messenger.messages.messaging.sms.chat.meet.views.GradientTextView
import messenger.messages.messaging.sms.chat.meet.views.CustomRecyclerView


class ChatHistoryAdapter(
    mActivity: BaseHomeActivity,
    var mConversations: ArrayList<ConversationSmsModel>,
    recyclerView: CustomRecyclerView,
    itemClick: (Any) -> Unit,
) : BaseAdapter(mActivity, recyclerView, null, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {
    private var fontSize = mActivity.getTextSize()
    private var drafts = HashMap<Long, String?>()
    private val BANNER_AD = 1
    private val CONTENT = 2
    var IS_AD_SHOWN = false

    init {
        setupDragListener(true)
        fetchDrafts(drafts)
    }

    override fun getActionMenuId() = R.menu.app_menu_cab_conversations

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newArrayList: ArrayList<ConversationSmsModel>) {
        val petDiffUtilCallback = PetDiffUtilCallback(mConversations, newArrayList)
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
            newList[newItemPosition].threadId == oldList[oldItemPosition].threadId


        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            newList[newItemPosition] == oldList[oldItemPosition]
    }

    override fun prepareActionMode(menu: Menu) {
        val selectedItems = getSelectedItems()

        menu.apply {
            findItem(R.id.menu_cab_block_number).isVisible = isNougatPlus()
            findItem(R.id.menu_cab_add_number_to_contact).isVisible = isOneItemSelected() && selectedItems.firstOrNull()?.isGroupConversation == false
            findItem(R.id.menu_cab_dial_number).isVisible = isOneItemSelected() && selectedItems.firstOrNull()?.isGroupConversation == false
            findItem(R.id.menu_menu_cab_copy_number).isVisible = isOneItemSelected() && selectedItems.firstOrNull()?.isGroupConversation == false
            findItem(R.id.menu_cab_mark_as_read).isVisible = selectedItems.any { !it.read }
            findItem(R.id.menu_cab_mark_as_unread).isVisible = selectedItems.any { it.read }
            checkPinBtnVisibility(this)
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.menu_cab_add_number_to_contact -> addNumberToContact()
            R.id.menu_cab_block_number -> askConfirmBlock()
            R.id.menu_cab_dial_number -> dialNumber()
            R.id.menu_menu_cab_copy_number -> copyNumberToClipboard()
            R.id.menu_cab_delete -> askConfirmDelete()
            R.id.menu_cab_mark_as_read -> markAsRead()
            R.id.menu_cab_mark_as_unread -> markAsUnread()
            R.id.menu_cab_pin_conversation -> pinConversation(true)
            R.id.menu_cab_unpin_conversation -> pinConversation(false)
            R.id.menu_cab_select_all -> selectAll()
        }
    }

    override fun getSelectableItemCount() = mConversations.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = mConversations.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = mConversations.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            BANNER_AD -> {
                Log.d("TAG_NATIVE", "onCreateViewHolder: ")
                val view1 = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_advance_native_banner, viewGroup, false)
                MyAdViewHolder1(view1)
            }

            CONTENT -> {
                val view1 = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_raw_chat_history, viewGroup, false)
                ViewHolder(view1)
            }

            else -> {
                val view1 = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_raw_chat_history, viewGroup, false)
                ViewHolder(view1)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyAdViewHolder1) {
            val adViewHolder = holder as MyAdViewHolder1
//                adViewHolder.binding.txtTitle.text = listVideo.get(position).dateString

            Log.d("TAG_NATIVE", "onBindViewHolder: ")
            adViewHolder.bindView(mConversations[0], true, false) { itemView, layoutPosition ->
                Log.d("TAG_NATIVE", "onBindViewHolder: ${!IS_AD_SHOWN}")
                if (!IS_AD_SHOWN) {
                    holder.itemView.apply {
                        if (!PrefClass.isProUser) {
                            mActivity.showBannerAds(findViewById(R.id.mBannerAdsContainer))
                        }else{
                            findViewById<ViewGroup>(R.id.mBannerAdsContainer)?.visibility = View.GONE
                        }
//                        messenger.messages.messaging.sms.chat.meet.ads.AdsManager.showNativeBannerAds(findViewById<NativeBannerAds>(R.id.mNativeAds), mActivity)
                    }
                    IS_AD_SHOWN = true
                }

            }
            adViewHolder.itemView.tag = holder
        } else if (holder is ViewHolder) {
            val holderMessage = holder as ViewHolder
            val conversation = mConversations[position]
            holderMessage.bindView(conversation, true, false) { itemView, layoutPosition ->
                setupView(itemView, conversation)
            }
            holderMessage.itemView.tag = holder
        }
    }


    override fun getItemCount() = mConversations.size

    private fun askConfirmBlock() {
        val numbers = getSelectedItems().distinctBy { it.phoneNumber }.map { it.phoneNumber }
        val numbersString = TextUtils.join(", ", numbers)
        val question = String.format(resources.getString(R.string.block_confirmation), numbersString)

        AlertDialogCustom(mActivity, question) {
            blockNumbers()
        }
    }

    private fun blockNumbers() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val numbersToBlock = getSelectedItems()
        val positions = getSelectedItemPositions()
        mConversations.removeAll(numbersToBlock)

        ensureBackgroundThread {
            numbersToBlock.map { it.phoneNumber }.forEach { number ->
                mActivity.addBlockedNumber(number)
            }

            mActivity.runOnUiThread {
                removeSelectedItems(positions)
                finishActMode()
            }
        }
    }

    private fun dialNumber() {
        val conversation = getSelectedItems().firstOrNull() ?: return
        mActivity.dialNumber(conversation.phoneNumber) {
            finishActMode()
        }
    }

    private fun copyNumberToClipboard() {
        val conversation = getSelectedItems().firstOrNull() ?: return
        mActivity.copyToClipboard(conversation.phoneNumber)
        finishActMode()
    }

    private fun askConfirmDelete() {
        val itemsCnt = selectedKeys.size
        val items = resources.getQuantityString(R.plurals.delete_conversations, itemsCnt, itemsCnt)

        val baseString = R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)

        AlertDialogCustom(mActivity, question) {
            ensureBackgroundThread {
                deleteConversations()
            }
        }
    }


    private fun deleteConversations() {
        if (selectedKeys.isEmpty()) {
            return
        }
        Handler(Looper.getMainLooper()).post {
        }


        val conversationsToRemove = mConversations.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<ConversationSmsModel>
        val positions = getSelectedItemPositions()
        conversationsToRemove.forEach {
            mActivity.deleteConversation(it.threadId)
            mActivity.notificationManager.cancel(it.hashCode())
        }

        try {
            mConversations.removeAll(conversationsToRemove)
        } catch (ignored: Exception) {
        }

        mActivity.runOnUiThread {
            Log.e("Event: ", "conversationsToRemove size: " + conversationsToRemove.size)
            if (conversationsToRemove.isEmpty()) {
                Handler(Looper.getMainLooper()).post {
                }
                Log.e("Event: ", "deleteConversations")
                refreshMessages()
                finishActMode()
            } else {
                Handler(Looper.getMainLooper()).post {
                }
                removeSelectedItems(positions)
                if (mConversations.isEmpty()) {
                    Log.e("Event: ", "deleteConversations is Empty")
                    mActivity.config.appRunCount = 1
                    refreshMessages()
                }
            }
        }
    }

    private fun markAsRead() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val conversationsMarkedAsRead = mConversations.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<ConversationSmsModel>
        ensureBackgroundThread {
            conversationsMarkedAsRead.filter { conversation -> !conversation.read }.forEach {
                mActivity.markThreadMessagesReadNew(it.threadId)
            }

            mActivity.runOnUiThread {
                Log.e("Event: ", "markAsRead")
                refreshMessages()

                finishActMode()
            }
        }
    }

    private fun markAsUnread() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val conversationsMarkedAsUnread = mConversations.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<ConversationSmsModel>
        ensureBackgroundThread {
            conversationsMarkedAsUnread.filter { conversation -> conversation.read }.forEach {
                mActivity.markThreadMessagesUnreadNew(it.threadId)
            }

            mActivity.runOnUiThread {
                Log.e("Event: ", "markAsUnread 2")
                refreshMessages()
                finishActMode()
            }
        }
    }

    private fun addNumberToContact() {
        val conversation = getSelectedItems().firstOrNull() ?: return
        Intent().apply {
            action = Intent.ACTION_INSERT_OR_EDIT
            type = "vnd.android.cursor.item/contact"
            putExtra(KEY_PHONE, conversation.phoneNumber)
            mActivity.launchActivityIntent(this)
        }
    }

    private fun getSelectedItems() = mConversations.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<ConversationSmsModel>

    private fun pinConversation(pin: Boolean) {
        val mConversations = getSelectedItems()
        if (mConversations.isEmpty()) {
            return
        }

        if (pin) {
            mActivity.config.addPinnedConversations(mConversations)
        } else {
            mActivity.config.removePinnedConversations(mConversations)
        }

        mActivity.runOnUiThread {
            Log.e("Event: ", "pinConversation")
            refreshMessages()
            finishActMode()
        }
    }

    private fun checkPinBtnVisibility(menu: Menu) {
        val pinnedConversations = mActivity.config.pinnedConversations
        val selectedConversations = getSelectedItems()
        menu.findItem(R.id.menu_cab_pin_conversation).isVisible = selectedConversations.any { !pinnedConversations.contains(it.threadId.toString()) }
        menu.findItem(R.id.menu_cab_unpin_conversation).isVisible = selectedConversations.any { pinnedConversations.contains(it.threadId.toString()) }
    }

    private fun fetchDrafts(drafts: HashMap<Long, String?>) {
        drafts.clear()
        for ((threadId, draft) in mActivity.getAllDrafts()) {
            drafts[threadId] = draft
        }
    }

    fun updateFontSize() {
        fontSize = mActivity.getTextSize()
        notifyDataSetChanged()
    }

    fun updateDrafts() {
        val newDrafts = HashMap<Long, String?>()
        fetchDrafts(newDrafts)
        if (drafts.hashCode() != newDrafts.hashCode()) {
            drafts = newDrafts
            notifyDataSetChanged()
        }
    }

    @SuppressLint("CutPasteId")
    private fun setupView(view: View, conversation: ConversationSmsModel) {
        view.apply {
            val smsDraft = drafts[conversation.threadId]
            findViewById<TextView>(R.id.tvDraftIndicator).beVisibleIf(smsDraft != null)

            findViewById<ImageView>(R.id.ivPinThumb).beVisibleIf(mActivity.config.pinnedConversations.contains(conversation.threadId.toString()))


            findViewById<TextView>(R.id.tvConversationTitle).apply {
                text = conversation.title
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            }

            findViewById<TextView>(R.id.tvConversationDesc).apply {
                text = smsDraft ?: conversation.snippet
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            }

            findViewById<GradientTextView>(R.id.tvDate).apply {
                text = conversation.date.formatDateOrTime(context, true, false)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (conversation.read) {
                    findViewById<GradientTextView>(R.id.tvDate).setGradientColors(context.getColor(R.color.text_grey), context.getColor(R.color.text_grey))
                    findViewById<ImageView>(R.id.imgUnread).isVisible = false
                } else {
                    findViewById<GradientTextView>(R.id.tvDate).setGradientColors(context.getColor(R.color.blue), context.getColor(R.color.purple))
                    findViewById<ImageView>(R.id.imgUnread).isVisible = true
                }
            }
            val firstChar: Char = conversation.title[0]
            if (Utils.isAlphabet(firstChar)) {
                findViewById<ImageView>(R.id.ivThumb).isVisible = false
                findViewById<TextView>(R.id.txtFirstLetter).isVisible = true
                findViewById<TextView>(R.id.txtFirstLetter).text = firstChar.toString()
            } else {
                findViewById<ImageView>(R.id.ivThumb).isVisible = true
                findViewById<TextView>(R.id.txtFirstLetter).isVisible = false
            }
        }
    }

    override fun onChange(position: Int) = mConversations.getOrNull(position)?.title ?: ""

    override fun getItemViewType(position: Int): Int {
        return if (mConversations[position].date == 1L) {
            BANNER_AD
        } else {
            CONTENT
        }
    }


    internal class MyAdViewHolder1(
        private val view: View
    ) :
        RecyclerView.ViewHolder(view) {
        fun bindView(any: Any, allowSingleClick: Boolean, allowLongClick: Boolean, callback: (itemView: View, layoutPosition: Int) -> Unit): View {
            return view.apply {
                callback(this, layoutPosition)
                if (allowSingleClick) {
                    setOnClickListener { viewClicked(any) }
                    setOnLongClickListener {
                        if (allowLongClick)
                            viewLongClicked()
                        else
                            viewClicked(any);
                        true
                    }
                } else {
                    setOnClickListener(null)
                    setOnLongClickListener(null)
                }
            }
        }

        fun viewClicked(any: Any) {

        }

        fun viewLongClicked() {

        }
    }
}
