package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import messenger.messages.messaging.sms.chat.meet.extensions.getTextSize
import messenger.messages.messaging.sms.chat.meet.extensions.highlightTextPart
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.extensions.formatDateOrTime
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.model.SearchModel
import messenger.messages.messaging.sms.chat.meet.views.CustomRecyclerView

class SearchAdapter(
    activity: BaseHomeActivity,
    var mSearchResults: ArrayList<SearchModel>,
    recyclerView: CustomRecyclerView,
    highlightText: String,
    itemClick: (Any) -> Unit
) : BaseAdapter(activity, recyclerView, null, itemClick) {

    private var fontSize = activity.getTextSize()
    private var textToHighlight = highlightText

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

        val view1 = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_raw_chat_history_by_search, viewGroup, false)
        return ViewHolder(view1)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is ViewHolder) {
            val searchResult = mSearchResults[position]
            holder.bindView(searchResult, true, false) { itemView, layoutPosition ->
                setupView(itemView, searchResult)
            }

        }
        holder.itemView.tag = holder
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateFontSize() {
        if (fontSize.equals(mActivity.getTextSize())) {
            return
        }
        fontSize = mActivity.getTextSize()
        notifyDataSetChanged()
    }


    override fun getItemCount() = mSearchResults.size

    fun updateItems(newItems: ArrayList<SearchModel>, highlightText: String = "") {
        if (newItems.hashCode() != mSearchResults.hashCode()) {
            mSearchResults = newItems.clone() as ArrayList<SearchModel>
            textToHighlight = highlightText
            notifyDataSetChanged()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
    }

    @SuppressLint("CutPasteId")
    private fun setupView(view: View, searchResult: SearchModel) {
        view.apply {
            findViewById<TextView>(R.id.tvConversationTitle).apply {
                text = searchResult.title!!.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            }

            findViewById<TextView>(R.id.tvConversationDesc).apply {
                text = searchResult.snippet!!.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            }

            findViewById<TextView>(R.id.tvConversationDate).apply {
                val date = searchResult.date?.toLong()?.formatDateOrTime(mActivity!!, true, true)
                text = date
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
            }

            val firstChar: Char = (searchResult.title?.get(0) ?: "+") as Char
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

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (!mActivity.isDestroyed && !mActivity.isFinishing && holder.itemView.findViewById<ImageView>(R.id.ivThumb) != null) {
            Glide.with(mActivity).clear(holder.itemView.findViewById<ImageView>(R.id.ivThumb))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mSearchResults[position].date!!.isEmpty() && mSearchResults[position].title!!.isEmpty()) {
            1
        } else 2
    }

}
