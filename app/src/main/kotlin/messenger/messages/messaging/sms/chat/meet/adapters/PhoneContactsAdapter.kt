package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.os.Build
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import messenger.messages.messaging.sms.chat.meet.extensions.getTextSize
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.views.FastScrolling
import messenger.messages.messaging.sms.chat.meet.model.ContactsModel
import messenger.messages.messaging.sms.chat.meet.views.CustomRecyclerView
import messenger.messages.messaging.sms.chat.meet.views.GradientTextView
import java.util.*
import kotlin.collections.ArrayList

class PhoneContactsAdapter(
    mActivity: BaseHomeActivity,
    var mContacts: ArrayList<ContactsModel>,
    recyclerView: CustomRecyclerView,
    fastScroller: FastScrolling?,
    itemClick: (Any) -> Unit,
) : BaseAdapter(mActivity, recyclerView, fastScroller, itemClick) {
    private var fontSize = mActivity.getTextSize()
    private var firstCharSerialWise = ""
    private val firstCharList: ArrayList<String> = ArrayList()
    private val firstCharAddedPosList: ArrayList<Int> = ArrayList()
    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = mContacts.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = mContacts.getOrNull(position)?.rawId

    override fun getItemKeyPosition(key: Int) = mContacts.indexOfFirst { it.rawId == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.list_raw_new_conversation, parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            val contact = mContacts[position]
            holder.bindView(contact, true, false) { itemView, layoutPosition ->
                setupView(itemView, contact, position)
            }
            bindViewHolder(holder)
        }
    }

    override fun getItemCount() = mContacts.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateContacts(newContacts: ArrayList<ContactsModel>) {
        val oldHashCode = mContacts.hashCode()
        val newHashCode = newContacts.hashCode()
        firstCharList.clear()
        firstCharAddedPosList.clear()
        firstCharSerialWise = ""
        if (newHashCode != oldHashCode) {
            mContacts = newContacts
            notifyDataSetChanged()
        }
    }

    @SuppressLint("CutPasteId", "SetTextI18n")
    private fun setupView(view: View, contact: ContactsModel, position: Int) {
        view.apply {
            findViewById<TextView>(R.id.tvConversationTitle).apply {
                text = contact.name
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            }

            findViewById<TextView>(R.id.tvConversationDate).visibility = View.GONE

            findViewById<TextView>(R.id.tvConversationDesc).apply {
                text = "Mobile " + contact.phoneNumbers.first()
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            }

            val firstChar: Char = contact.name[0]


            if (Utils.isAlphabet(firstChar)) {
                findViewById<ImageView>(R.id.ivThumb).isVisible = false
                findViewById<TextView>(R.id.txtFirstLetter).isVisible = true
                findViewById<TextView>(R.id.txtFirstLetter).text = firstChar.toString()
            } else {
                findViewById<ImageView>(R.id.ivThumb).isVisible = true
                findViewById<TextView>(R.id.txtFirstLetter).isVisible = false
            }

            if (firstChar.toString().uppercase() != firstCharSerialWise && !firstCharList.contains(
                    firstChar.toString().uppercase()
                ) || firstCharAddedPosList.contains(position)
            ) {
                firstCharSerialWise = firstChar.toString().uppercase()
                firstCharList.add(firstCharSerialWise)
                firstCharAddedPosList.add(position)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    findViewById<GradientTextView>(R.id.txtSerialWiseChar).setGradientColors(context.getColor(R.color.blue), context.getColor(R.color.purple))
                }
                findViewById<GradientTextView>(R.id.txtSerialWiseChar).text = firstChar.toString().uppercase()
            } else {
                findViewById<GradientTextView>(R.id.txtSerialWiseChar).text = ""
            }

        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (!mActivity.isDestroyed && !mActivity.isFinishing) {
            Glide.with(mActivity).clear(holder.itemView.findViewById<ImageView>(R.id.ivThumb))
        }
    }
}
