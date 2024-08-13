package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.extensions.copyToClipboard
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseActivity
import messenger.messages.messaging.sms.chat.meet.send_message.Utils
import messenger.messages.messaging.sms.chat.meet.listners.RefreshingRecyclerListner
import messenger.messages.messaging.sms.chat.meet.model.BlockContactModel
import messenger.messages.messaging.sms.chat.meet.model.UnBlockMsgListModel
import messenger.messages.messaging.sms.chat.meet.utils.blockContactDao
import messenger.messages.messaging.sms.chat.meet.utils.refreshMessages
import messenger.messages.messaging.sms.chat.meet.views.CustomRecyclerView
import org.greenrobot.eventbus.EventBus

class BlockedNumberAdapter(
    activity: BaseActivity, var blockedNumbers: ArrayList<BlockContactModel>, val mListener: RefreshingRecyclerListner?,
    recyclerView: CustomRecyclerView, itemClick: (Any) -> Unit, val itemLongClick: (Int) -> Unit
) : BaseAdapter(activity, recyclerView, null, itemClick) {
    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.app_menu_cab_blocked_numbers

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.menu_menu_cab_copy_number).isVisible = isOneItemSelected()
        }
        Log.d("TAG_MENU", "prepareActionMode: ${selectedKeys.size}")
        itemLongClick(selectedKeys.size)
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.menu_menu_cab_copy_number -> copyNumberToClipboard()
            R.id.menu_cab_delete -> deleteSelection()
        }
    }

    override fun getSelectableItemCount() = blockedNumbers.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = blockedNumbers.getOrNull(position)?.blockID?.toInt()

    override fun getItemKeyPosition(key: Int) = blockedNumbers.indexOfFirst { it.blockID.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.layout_block_no, parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val blockedNumber = blockedNumbers[position]
        if (holder is ViewHolder) {
            holder.bindView(blockedNumber, true, true) { itemView, layoutPosition ->
                setupView(itemView, blockedNumber)
            }
            bindViewHolder(holder)
        }
    }

    override fun getItemCount() = blockedNumbers.size

    private fun getSelectedItems() = blockedNumbers.filter { selectedKeys.contains(it.blockID.toInt()) } as ArrayList<BlockContactModel>

    @SuppressLint("CutPasteId")
    private fun setupView(view: View, blockedNumber: BlockContactModel) {
        view.apply {
//            manage_blocked_number_holder?.isSelected = selectedKeys.contains(blockedNumber.id.toInt())

            if (selectedKeys.contains(blockedNumber.blockID.toInt())) {
                findViewById<LinearLayout>(R.id.manage_blocked_number_holder).setBackgroundColor(mActivity.resources.getColor(R.color.activated_item_foreground))
            } else {
                findViewById<LinearLayout>(R.id.manage_blocked_number_holder).setBackgroundColor(mActivity.resources.getColor(R.color.transparent))
            }

            findViewById<TextView>(R.id.manage_blocked_number_title).apply {
                text = blockedNumber.number
            }

            val firstChar: Char = blockedNumber.number[0]
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

    private fun copyNumberToClipboard() {
        val selectedNumber = getSelectedItems().firstOrNull() ?: return
        mActivity.copyToClipboard(selectedNumber.number)
        finishActMode()
    }

    private fun deleteSelection() {
        val deleteBlockedNumbers = ArrayList<BlockContactModel>(selectedKeys.size)
        val positions = getSelectedItemPositions()

        CoroutineScope(Dispatchers.IO).launch {

            getSelectedItems().forEach {
                deleteBlockedNumbers.add(it)
                mActivity.blockContactDao.deleteByThreadId(it.threadID)
            }

            CoroutineScope(Dispatchers.Main).launch {
                blockedNumbers.removeAll(deleteBlockedNumbers)
                selectedKeys.clear()
                itemLongClick.invoke(selectedKeys.size)
                removeSelectedItems(positions)
                Log.d("TAG_REFRESH", "deleteSelection: ")

                EventBus.getDefault().post(UnBlockMsgListModel(deleteBlockedNumbers))
                /*if (blockedNumbers.isEmpty() || blockedNumbers.size == 0) {
                }*/
                mListener?.refreshItems()
            }
        }

    }
}
