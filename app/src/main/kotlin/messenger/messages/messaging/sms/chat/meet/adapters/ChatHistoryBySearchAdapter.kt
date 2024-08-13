package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.extensions.formatDateOrTime
import messenger.messages.messaging.sms.chat.meet.views.CustomRecyclerView
import messenger.messages.messaging.sms.chat.meet.extensions.getTextSize
import messenger.messages.messaging.sms.chat.meet.extensions.highlightTextPart
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.model.SearchModel
import messenger.messages.messaging.sms.chat.meet.utils.getContactNameFromPhoneNumber
import naimishtrivedi.`in`.googleadsmanager.NativeBannerAds

class ChatHistoryBySearchAdapter(
    activity: BaseHomeActivity,
    var mSearchResults: ArrayList<SearchModel>,
    recyclerView: CustomRecyclerView,
    highlightText: String,
    itemClick: (Any) -> Unit,
) : BaseAdapter(activity, recyclerView, null, itemClick) {

    private var fontSize = activity.getTextSize()
    private var textToHighlight = highlightText
    private val BANNER_AD = 1
    private val CONTENT = 2
    var IS_AD_SHOWN = false

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = mSearchResults.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = mSearchResults.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = mSearchResults.indexOfFirst { it.hashCode() == key }

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
                val view1 = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_raw_chat_history_by_search, viewGroup, false)
                ViewHolder(view1)
            }

            else -> {
                val view1 = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_raw_chat_history_by_search, viewGroup, false)
                ViewHolder(view1)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyAdViewHolder1) {
            //                adViewHolder.binding.txtTitle.text = listVideo.get(position).dateString

            Log.d("TAG_NATIVE", "onBindViewHolder: ")
            holder.bindView(mSearchResults[0], true, false) { itemView, layoutPosition ->
                Log.d("TAG_NATIVE", "onBindViewHolder: ${!IS_AD_SHOWN}")
                if (!IS_AD_SHOWN) {
                    holder.itemView.apply {
                        messenger.messages.messaging.sms.chat.meet.ads.AdsManager.showNativeBannerAds(findViewById<NativeBannerAds>(R.id.mNativeAds), mActivity)
                    }
                    IS_AD_SHOWN = true
                }

            }
            holder.itemView.tag = holder
        } else if (holder is ViewHolder) {
            val searchResult = mSearchResults[position]
            holder.bindView(searchResult, true, false) { itemView, layoutPosition ->
                setupView(itemView, searchResult)
            }

        }
        holder.itemView.tag = holder
    }

    fun updateFontSize() {
        if (fontSize.equals(mActivity.getTextSize())) {
            return
        }
        fontSize = mActivity.getTextSize()
        notifyDataSetChanged()
    }


    override fun getItemCount() = mSearchResults.size


    @SuppressLint("CutPasteId")
    private fun setupView(view: View, searchResult: SearchModel) {
        view.apply {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val name: String
                    withContext(Dispatchers.IO) {
                        name = mActivity.getContactNameFromPhoneNumber(searchResult.phoneNumber.toString()).ifEmpty {
                            searchResult.title.toString()
                        }
                    }
                    findViewById<TextView>(R.id.tvConversationTitle).apply {
                        text = name.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
                    }

                    findViewById<TextView>(R.id.tvConversationDesc).apply {
                        text = searchResult.snippet?.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
                    }

                    findViewById<TextView>(R.id.tvConversationDate).apply {
                        val date = searchResult.date?.toLong()?.formatDateOrTime(mActivity!!, true, true)
                        text = date
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
                    }

                    val firstChar: Char = if (name.isNotEmpty()) {
                        name[0]
                    } else {
                        Character.MIN_VALUE
                    }
                    if (Utils.isAlphabet(firstChar)) {
                        findViewById<ImageView>(R.id.ivThumb).isVisible = false
                        findViewById<TextView>(R.id.txtFirstLetter).isVisible = true
                        findViewById<TextView>(R.id.txtFirstLetter).text = firstChar.toString()
                    } else {
                        findViewById<ImageView>(R.id.ivThumb).isVisible = true
                        findViewById<TextView>(R.id.txtFirstLetter).isVisible = false
                    }
                } catch (e: Exception) {
                    Log.d("TAG_ERROR", "setupView: ${e.message}")
                }
            }

        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mSearchResults[position].date == "1") {
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
