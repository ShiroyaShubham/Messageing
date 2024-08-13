package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
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
import messenger.messages.messaging.sms.chat.meet.extensions.beGone
import messenger.messages.messaging.sms.chat.meet.extensions.beVisibleIf
import messenger.messages.messaging.sms.chat.meet.extensions.formatDateOrTime
import messenger.messages.messaging.sms.chat.meet.extensions.getTextSize
import messenger.messages.messaging.sms.chat.meet.model.SearchModel
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.utils.config
import messenger.messages.messaging.sms.chat.meet.utils.getAllDrafts
import messenger.messages.messaging.sms.chat.meet.utils.getContactNameFromPhoneNumber

class ChatHistoryBySearchAdapter1(val mActivity: BaseHomeActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var mConversations: ArrayList<SearchModel> = arrayListOf()
    private var drafts = HashMap<Long, String?>()
    private var fontSize = mActivity.getTextSize()

    init {
        fetchDrafts(drafts)
    }

    var IS_AD_SHOWN = false
    private val BANNER_AD = 1
    private val CONTENT = 2

    var isShowSelection: Boolean = false

    lateinit var itemClickListener: ItemClickListener<SearchModel>
    lateinit var itemClickListenerSelect: ItemClickListener1<SearchModel>

    lateinit var itemLongClickListener: ItemClickListener<SearchModel>

    inner class ViewHolder(private val bind: ListRawChatHistoryBinding) : RecyclerView.ViewHolder(bind.root) {
        fun bindData(position: Int) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val conversation = mConversations[position]
//                    val smsDraft = drafts[conversation.threadId]
//
//                    Log.e(
//                        "TAG_CONVERSATION",
//                        "bindData: ${conversation.title} ${conversation.read} ${position}"
//                    )

                    var name: String
                    withContext(Dispatchers.IO) {
                        name = mActivity.getContactNameFromPhoneNumber(conversation.phoneNumber!!).ifEmpty {
                            conversation.title!!
                        }
                    }
                    bind.tvConversationTitle.apply {
                        text = name
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
                    }

                    bind.tvConversationDesc.apply {
                        text = conversation.snippet
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
                    }

                    bind.tvDate.apply {
                        text = conversation.date?.toLong()?.formatDateOrTime(context, true, false)
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
                    }

                    bind.tvDraftIndicator.beGone()
                    bind.ivPinThumb.beVisibleIf(mActivity.config.pinnedConversations.contains(conversation.threadId.toString()))
                    bind.tvConversationTitle.text = name
                    bind.tvConversationDesc.text = conversation.snippet
                    bind.tvDate.text = conversation.date?.toLong()?.formatDateOrTime(mActivity, true, false)


                    if (name.isNotEmpty() == true) {
                        val firstChar: Char = name[0]
                        if (Utils.isAlphabet(firstChar)) {
                            bind.ivThumb.isVisible = false
                            bind.txtFirstLetter.isVisible = true
                            bind.txtFirstLetter.text = firstChar.toString()
                        } else {
                            bind.ivThumb.isVisible = true
                            bind.txtFirstLetter.isVisible = false
                        }
                    }

//            if (itemView.context.getSharedPrefs().getBoolean("SHOW_PROFILE", false)) {
//                if (!conversation.photoUri.equals("")) {
//                    Glide.with(mActivity).load(conversation.photoUri)
//                        .into(bind.imgProfile)
//                    bind.imgProfile.apply {
//                        visibility = View.VISIBLE
//                    }
//                }
//            }

                    bind.rlMain.setOnClickListener {
                        /*val intent = Intent(mActivity, MsgActivity::class.java)
                    intent.putExtra(THREAD_ID, conversation.threadId)
                    intent.putExtra(THREAD_TITLE, conversation.title)
                    intent.putExtra(THREAD_NUMBER, conversation.phoneNumber)
                    mActivity.startActivity(intent)*/
                        itemClickListenerSelect(absoluteAdapterPosition, conversation)
                    }

//            bind.rlMain.setOnLongClickListener {
//                itemLongClickListener(absoluteAdapterPosition, conversation, true)
//                return@setOnLongClickListener true
//            }

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

                /*bind.mNativeAds.showAds(PreferenceHelper.getString(AdConstants.NATIVE, ""))
                bind.mNativeAds.setOnAdsViewListener(object : OnAdsViewListener {
                    override fun onAdsSuccess(adResource: String) {
                        Log.e("TAG", "$adResource Success")
                        IS_AD_SHOWN = true
                    }

                    override fun onAdsFailure(adResource: String, errorMsg: String) {
                        Log.e("TAG", "$adResource Error msg $errorMsg")
                        IS_AD_SHOWN = false
                    }
                })
                bind.mNativeAds.setOnAdRevenueListener(OnAdRevenueListener { model ->
                    val logPaidImpression = LogPaidImpression()
                    logPaidImpression.adsPlatform = "ad_manager" //Set Ads Platform value
                    logPaidImpression.adsPlacement = model.adsPlacement //Set Ads Placement value like (banner, reward, native, interstitial or reward)
                    logPaidImpression.adsSourceName = model.adsSourceName //Set Ads source name value from AdsPaidModel
                    logPaidImpression.adsUnitId = model.adsUnitId // Set Ads unit id value from AdsPaidModel
                    logPaidImpression.currencyCode = model.currencyCode //Set Currency code value from AdsPaidModel
                    logPaidImpression.valueMicros = model.valueMicros //Set value micros from AdsPaidModel
                    AdsManager.logPaidImpression(mActivity, logPaidImpression)
                })*/

                if (mActivity is BaseActivity) {
                    IS_AD_SHOWN = true
                    itemView.apply {
                        messenger.messages.messaging.sms.chat.meet.ads.AdsManager.showNativeBannerAds(
                            findViewById(
                                R.id.mNativeAds
                            ), mActivity
                        )
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

    fun getFileListData(): ArrayList<SearchModel> {
        return mConversations
    }

//    fun getFileListDataSelected(): ArrayList<SearchModel> {
//        return mConversations.filter { it.isSelected } as ArrayList<SearchModel>
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

//    private fun getSelectedItemsPin() = mConversations.filter { it.isSelected } as ArrayList<SearchModel>


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            BANNER_AD -> {
                ViewHolderAds(
                    ItemAdvanceNativeBannerBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            CONTENT -> {
                ViewHolder(
                    ListRawChatHistoryBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                ViewHolder(
                    ListRawChatHistoryBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
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
        Log.d("TAG_ADS_ITEM", "getItemViewType: ${mConversations[position].date}")
        return if (mConversations[position].date == "1") {
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

    fun setData(newArrayList: ArrayList<SearchModel>) {
        val petDiffUtilCallback = PetDiffUtilCallback(mConversations, newArrayList)
        val diffResult = DiffUtil.calculateDiff(petDiffUtilCallback)
        mConversations.clear()
        mConversations.addAll(newArrayList)
        diffResult.dispatchUpdatesTo(this)
    }

    class PetDiffUtilCallback(
        private val oldList: ArrayList<SearchModel>,
        private val newList: ArrayList<SearchModel>
    ) :
        DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            if (newList.isNotEmpty() && oldList.isNotEmpty()) newList[newItemPosition].threadId == oldList[oldItemPosition].threadId else true


        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            if (newList.isNotEmpty() && oldList.isNotEmpty()) newList[newItemPosition] == oldList[oldItemPosition] else true
    }
}
