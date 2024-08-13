package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import messenger.messages.messaging.sms.chat.meet.extensions.highlightTextPart
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.model.SearchModel
import messenger.messages.messaging.sms.chat.meet.views.CustomRecyclerView
import java.util.*

class SuggestionAdapter(
    mActivity: BaseHomeActivity,
    var searchResults: ArrayList<SearchModel>,
    recyclerView: CustomRecyclerView,
    itemClick: (Any) -> Unit,
) : BaseAdapter(mActivity, recyclerView, null, itemClick) {

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = searchResults.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = searchResults.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = searchResults.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.list_raw_suggestions, parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val searchResult = searchResults[position]
        if(holder is ViewHolder) {
            holder.bindView(searchResult, true, false) { itemView, layoutPosition ->
                setupView(itemView, searchResult)
            }
            bindViewHolder(holder)
        }
    }

    override fun getItemCount() = searchResults.size

    @SuppressLint("CutPasteId")
    private fun setupView(view: View, searchResult: SearchModel) {
        view.apply {

            val paddingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics).toInt()
            findViewById<LinearLayout>(R.id.llMain).setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

            findViewById<TextView>(R.id.tvTitle).apply {
                text = searchResult.title!!.highlightTextPart("", adjustedPrimaryColor)
            }

            val firstChar: Char = (searchResult.title?.get(0) ?: "+") as Char
            if (Utils.isAlphabet(firstChar)) {
                findViewById<ImageView>(R.id.ivThumb).isVisible = false
                findViewById<TextView>(R.id.txtFirstLetter).isVisible = true
                findViewById<TextView>(R.id.txtFirstLetter).text = firstChar.toString()
            }else{
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
}
