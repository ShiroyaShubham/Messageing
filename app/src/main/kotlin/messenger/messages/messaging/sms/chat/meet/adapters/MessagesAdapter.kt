package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.telephony.SubscriptionManager
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import messenger.messages.messaging.sms.chat.meet.MainAppClass
import messenger.messages.messaging.sms.chat.meet.extensions.*
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.activity.BaseHomeActivity
import messenger.messages.messaging.sms.chat.meet.activity.CopingTextActivity
import messenger.messages.messaging.sms.chat.meet.activity.HomeActivity
import messenger.messages.messaging.sms.chat.meet.utils.deleteMessage
import messenger.messages.messaging.sms.chat.meet.utils.updateLastConversationMessage
import messenger.messages.messaging.sms.chat.meet.views.FastScrolling
import messenger.messages.messaging.sms.chat.meet.dialogs.AlertDialogCustom
import messenger.messages.messaging.sms.chat.meet.model.*
import messenger.messages.messaging.sms.chat.meet.utils.*
import messenger.messages.messaging.sms.chat.meet.views.CustomRecyclerView
import org.greenrobot.eventbus.EventBus
import java.util.regex.Pattern


class MessagesAdapter(
    mActivity: BaseHomeActivity, var mMessages: ArrayList<ItemModel>, recyclerView: CustomRecyclerView, fastScroller: FastScrolling,
    itemClick: (Any) -> Unit, val onItemLongClick: (Any) -> Unit,
) : BaseAdapter(mActivity, recyclerView, fastScroller, itemClick) {
    private val roundedCornersRadius = resources.getDimension(R.dimen.margin_12).toInt()
    private var fontSize = mActivity.getTextSize()
    private var messagesLauncher = ArrayList<ItemModel>()
    private var deleteLuancherListner: DeleteLuancherListner? = null

    @SuppressLint("MissingPermission")
    private val hasMultipleSIMCards = SubscriptionManager.from(mActivity).activeSubscriptionInfoList?.size ?: 0 > 1

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.app_menu_cab_thread

    override fun prepareActionMode(menu: Menu) {
        val isOneItemSelected = isOneItemSelected()
        menu.apply {
            findItem(R.id.menu_cab_copy_to_clipboard).isVisible = isOneItemSelected
            findItem(R.id.menu_cab_share).isVisible = isOneItemSelected
            findItem(R.id.menu_cab_select_text).isVisible = isOneItemSelected
        }
        onItemLongClick.invoke(selectedKeys.size)
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.menu_cab_copy_to_clipboard -> copyToClipboard()
            R.id.menu_cab_share -> shareText()
//            R.id.menu_cab_select_text -> copyText()
            R.id.menu_cab_delete -> askConfirmDelete()
            R.id.menu_cab_select_all -> selectAll()
        }
    }

    override fun getSelectableItemCount() = mMessages.filter { it is MessagesModel }.size

    override fun getIsItemSelectable(position: Int) = !isThreadDateTime(position)

    override fun getItemSelectionKey(position: Int) = (mMessages.getOrNull(position) as? MessagesModel)?.hashCode()

    override fun getItemKeyPosition(key: Int) = mMessages.indexOfFirst { (it as? MessagesModel)?.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = when (viewType) {
            THREAD_DATE_TIME -> R.layout.list_raw_sms_date_and_time
            THREAD_RECEIVED_MESSAGE -> R.layout.list_raw_get_sms
            THREAD_SENT_MESSAGE_ERROR -> R.layout.list_raw_send_sms_error
            THREAD_SENT_MESSAGE_SENT -> R.layout.list_raw_send_sms_success
            THREAD_SENT_MESSAGE_SENDING -> R.layout.list_raw_sms_status_sending
            else -> R.layout.list_raw_sent_sms
        }
        return createViewHolder(layout, parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = mMessages[position]
        val isClickable = item is ExeptionModel || item is MessagesModel
        val isLongClickable = item is MessagesModel
        if (holder is ViewHolder) {
            holder.bindView(item, isClickable, isLongClickable) { itemView, _ ->
                when (item) {
                    is DateAndTimeModel -> setupDateTime(itemView, item)
                    is SendedSmsModel -> setupThreadSuccess(itemView, item.delivered)
                    is ExeptionModel -> setupThreadError(itemView)
                    is SendSmsModel -> setupThreadSending(itemView)
                    else -> setupView(holder, itemView, item as MessagesModel)
                }
            }
            bindViewHolder(holder)
        }
    }

    override fun getItemCount() = mMessages.size

    override fun getItemViewType(position: Int): Int {
        val item = mMessages[position]
        return when {
            item is DateAndTimeModel -> THREAD_DATE_TIME
            (mMessages[position] as? MessagesModel)?.isReceivedMessage() == true -> THREAD_RECEIVED_MESSAGE
            item is ExeptionModel -> THREAD_SENT_MESSAGE_ERROR
            item is SendedSmsModel -> THREAD_SENT_MESSAGE_SENT
            item is SendSmsModel -> THREAD_SENT_MESSAGE_SENDING
            else -> THREAD_SENT_MESSAGE
        }
    }

    private fun copyToClipboard() {
        val firstItem = getSelectedItems().firstOrNull() as? MessagesModel ?: return
        mActivity.copyToClipboard(firstItem.body)
    }

    private fun shareText() {
        val firstItem = getSelectedItems().firstOrNull() as? MessagesModel ?: return
        mActivity.shareTextIntent(firstItem.body)
    }

    private fun copyText(message: MessagesModel) {
        if (message.body.trim().isNotEmpty()) {
            val intent = Intent(mActivity, CopingTextActivity::class.java)
            intent.putExtra(THREAD_TITLE, message.body)
        }
    }

    private fun selectText(message: MessagesModel, pos: Int) {
        if (message.body.trim().isNotEmpty()) {
            messagesLauncher.clear()
            messagesLauncher.add(message)

            deleteLuancherListner!!.onDeleteLuancherCall(message.body, pos)
        }
    }

    fun deleteLuancherListner(deleteListner: DeleteLuancherListner) {
        deleteLuancherListner = deleteListner
    }

    fun deleteMessagesLauncher(pos: Int) {
        val messagesToRemove = messagesLauncher
        if (messagesToRemove.isEmpty()) {
            return
        }

        val positionsList = ArrayList<Int>()
        positionsList.add(pos)

        val threadId = (messagesToRemove.firstOrNull() as? MessagesModel)?.threadId ?: return
        messagesToRemove.forEach {
            mActivity.deleteMessage((it as MessagesModel).id, it.isMMS)
        }
        mMessages.removeAll(messagesToRemove)
        val messageItem = if (messagesToRemove.isNotEmpty()) (messagesToRemove[0] as? MessagesModel ?: return) else null
        mActivity.updateLastConversationMessage(messageItem)

        mActivity.runOnUiThread {
            if (mMessages.filter { it is MessagesModel }.isEmpty()) {
                mActivity.finish()
            } else {
                removeSelectedItems(positionsList)
            }
            Log.e("TAG_REFRESH", " deleteMessagesLauncher")
            MainAppClass.getAllMessagesFromDb {
                refreshMessages()
            }
        }
    }


    private fun askConfirmDelete() {
        val itemsCnt = selectedKeys.size

        val items = try {
            resources.getQuantityString(R.plurals.delete_messages, itemsCnt, itemsCnt)
        } catch (e: Exception) {
            mActivity.showErrorToast(e)
            return
        }

        val baseString = R.string.app_delete_message
        val title = resources.getString(R.string.app_delete)
        val question = String.format(resources.getString(baseString), items)

        AlertDialogCustom(mActivity, title, question) {
            ensureBackgroundThread {
                deleteMessages()
            }
        }
    }

    private fun deleteMessages() {
        val messagesToRemove = getSelectedItems()
        if (messagesToRemove.isEmpty()) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {

            val positions = getSelectedItemPositions()
            val threadId = (messagesToRemove.firstOrNull() as? MessagesModel)?.threadId ?: return@launch
            messagesToRemove.forEach {
                mActivity.deleteMessage((it as MessagesModel).id, it.isMMS)
            }
            mMessages.removeAll(messagesToRemove)
            val messageItem = if (messagesToRemove.isNotEmpty()) (messagesToRemove[0] as? MessagesModel ?: return@launch) else null
            mActivity.updateLastConversationMessage(messageItem)
            selectedKeys.clear()
            onItemLongClick.invoke(selectedKeys.size)
            if (mMessages.filterIsInstance<MessagesModel>().isEmpty()) {
                mActivity.conversationsDB.deleteThreadIdMessage(threadId)
                MainAppClass.getConversationDataFromDB()
                EventBus.getDefault().post(LoadConversationsModel())
                withContext(Dispatchers.Main) {
                    mActivity.finish()
                }
                Log.d("TAG_THREAD_ID", "deleteMessages: $threadId")
            } else {
                withContext(Dispatchers.Main) {
                    removeSelectedItems(positions)
                }
            }
            Log.e("TAG_REFRESH", " deleteMessages ok")
            MainAppClass.getAllMessagesFromDb {
                refreshMessages()
            }
        }


    }

    private fun getSelectedItems() = mMessages.filter {
        selectedKeys.contains((it as? MessagesModel)?.hashCode() ?: 0)
    } as ArrayList<ItemModel>

    private fun isThreadDateTime(position: Int) = mMessages.getOrNull(position) is DateAndTimeModel

    fun updateMessages(newMessages: ArrayList<ItemModel>) {
        val latestMessages = newMessages.clone() as ArrayList<ItemModel>
        val oldHashCode = mMessages.hashCode()
        val newHashCode = latestMessages.hashCode()
        if (newHashCode != oldHashCode) {
            mMessages = latestMessages
            notifyDataSetChanged()
            recyclerView.scrollToPosition(mMessages.size - 1)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupView(holder: ViewHolder, view: View, message: MessagesModel) {
        view.apply {
            findViewById<ConstraintLayout>(R.id.conMainHolder).isSelected = selectedKeys.contains(message.hashCode())
            findViewById<TextView>(R.id.tvMessageBody).apply {
                text = message.body
                movementMethod = LinkMovementMethod.getInstance()
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
            }
            findViewById<TextView>(R.id.tvMessageBody).beVisibleIf(message.body.isNotEmpty())

            if (selectedKeys.contains(message.hashCode())) {
                findViewById<TextView>(R.id.tvMessageBody).setTextColor(context.resources.getColor(android.R.color.white))
                findViewById<TextView>(R.id.tvMessageBody).setBackgroundResource(R.drawable.bg_gradient_round_corner_10)
            } else {
                if (message.isReceivedMessage()) {
                    findViewById<TextView>(R.id.tvMessageBody).setTextColor(context.resources.getColor(R.color.text_black))
                    findViewById<TextView>(R.id.tvMessageBody).setBackgroundResource(R.drawable.bg_light_grey_round_corner_10)
                } else {
                    findViewById<TextView>(R.id.tvMessageBody).setTextColor(context.resources.getColor(R.color.text_black))
                    findViewById<TextView>(R.id.tvMessageBody).setBackgroundResource(R.drawable.bg_grey_stroke_round_corner_10)
                }
            }

            findViewById<TextView>(R.id.tvCopy).text = "Copy:"
            if (message.body.isNotEmpty() && message.isReceivedMessage() &&
                (message.body.lowercase().contains("otp") || message.body.lowercase().contains("code")
                    || message.body.lowercase().contains("pin"))
            ) {
                val pattern_4 = Pattern.compile("(\\d{4})")
                val matcher_4 = pattern_4.matcher(message.body)

                val pattern_5 = Pattern.compile("(\\d{5})")
                val matcher_5 = pattern_5.matcher(message.body)

                val pattern_6 = Pattern.compile("(\\d{6})")
                val matcher_6 = pattern_6.matcher(message.body)

                if (matcher_6.find() && matcher_6.group(0).length == 6) {
                    findViewById<TextView>(R.id.tvCopyOTP).text = matcher_6.group(0)
                    findViewById<LinearLayout>(R.id.llCopyOTP).visibility = View.VISIBLE
                } else if (matcher_5.find() && matcher_5.group(0).length == 5) {
                    findViewById<TextView>(R.id.tvCopyOTP).text = matcher_5.group(0)
                    findViewById<LinearLayout>(R.id.llCopyOTP).visibility = View.VISIBLE
                } else if (matcher_4.find() && matcher_4.group(0).length == 4) {
                    findViewById<TextView>(R.id.tvCopyOTP).text = matcher_4.group(0)
                    findViewById<LinearLayout>(R.id.llCopyOTP).visibility = View.VISIBLE
                } else {
                    findViewById<LinearLayout>(R.id.llCopyOTP).visibility = View.GONE
                }
            } else {
                findViewById<LinearLayout>(R.id.llCopyOTP).visibility = View.GONE
            }

            findViewById<LinearLayout>(R.id.llCopyOTP).setOnClickListener {
                mActivity.copyToClipboard(findViewById<TextView>(R.id.tvCopyOTP).text.toString())
            }

            if (message.isReceivedMessage()) {
                findViewById<TextView>(R.id.tvMessageBody).setLinkTextColor(context.resources.getColor(R.color.text_link))
            } else {
                findViewById<TextView>(R.id.tvMessageBody).setLinkTextColor(context.resources.getColor(R.color.text_link))
            }

            setOnLongClickListener {
                holder.viewLongClicked()
                true
            }

            findViewById<TextView>(R.id.tvMessageBody).setOnTouchListener(object : OnTouchListener {
                @SuppressLint("ClickableViewAccessibility")
                private val gestureDetector = GestureDetector(mActivity, object : SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        copyText(message)
                        return super.onDoubleTap(e)
                    }

                    @SuppressLint("ClickableViewAccessibility")
                    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                        selectText(message, holder.layoutPosition)
                        return false
                    }
                })

                @SuppressLint("ClickableViewAccessibility")
                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    gestureDetector.onTouchEvent(event)
                    return true
                }
            })

            findViewById<LinearLayout>(R.id.llHolder).removeAllViews()
            if (message.attachment?.attachments?.isNotEmpty() == true) {
                for (attachment in message.attachment.attachments) {
                    val mimetype = attachment.mimetype
                    val uri = attachment.getUri()
                    if (mimetype.startsWith("image/") || mimetype.startsWith("video/")) {
                        val imageView = layoutInflater.inflate(R.layout.list_raw_attach_photo, null)
                        findViewById<LinearLayout>(R.id.llHolder).addView(imageView)

                        val isTallImage = attachment.height > attachment.width
                        val transformation = if (isTallImage) CenterCrop() else FitCenter()
                        val options = RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .transform(transformation, RoundedCorners(roundedCornersRadius))

                        var builder = Glide.with(context)
                            .load(uri)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .apply(options)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                    findViewById<TextView>(R.id.tvPlayVideo).beGone()
                                    findViewById<LinearLayout>(R.id.llHolder).removeView(imageView)
                                    return false
                                }

                                override fun onResourceReady(dr: Drawable?, a: Any?, t: Target<Drawable>?, d: DataSource?, i: Boolean) =
                                    false
                            })

                        if (isTallImage) {
                            builder = builder.override(attachment.width, attachment.width)
                        }

                        builder.into(imageView.findViewById(R.id.attachment_image))
                        imageView.findViewById<ImageView>(R.id.attachment_image).setOnClickListener {
                            if (actModeCallback.isSelectable) {
                                holder.viewClicked(message)
                            } else {
                                launchViewIntent(uri, mimetype, attachment.filename)
                            }
                        }
                        imageView.setOnLongClickListener {
                            holder.viewLongClicked()
                            true
                        }
                    } else {
                        if (message.isReceivedMessage()) {
                            val attachmentView = layoutInflater.inflate(R.layout.list_raw_received_unknown_attach, null).apply {
                                findViewById<TextView>(R.id.thread_received_attachment_label).apply {
                                    if (attachment.filename.isNotEmpty()) {
                                        findViewById<TextView>(R.id.thread_received_attachment_label).text = attachment.filename
                                    }
                                    setOnClickListener {
                                        if (actModeCallback.isSelectable) {
                                            holder.viewClicked(message)
                                        } else {
                                            launchViewIntent(uri, mimetype, attachment.filename)
                                        }
                                    }
                                    setOnLongClickListener {
                                        holder.viewLongClicked()
                                        true
                                    }
                                }
                            }
                            findViewById<LinearLayout>(R.id.llHolder).addView(attachmentView)
                        } else {
                            val background = context.getAdjustedPrimaryColor()
                            val attachmentView = layoutInflater.inflate(R.layout.list_raw_sent_unknown_attach, null).apply {
                                findViewById<TextView>(R.id.thread_sent_attachment_label).apply {
                                    this.background.applyColorFilter(background)
                                    if (attachment.filename.isNotEmpty()) {
                                        findViewById<TextView>(R.id.thread_sent_attachment_label).text = attachment.filename
                                    }
                                    setOnClickListener {
                                        if (actModeCallback.isSelectable) {
                                            holder.viewClicked(message)
                                        } else {
                                            launchViewIntent(uri, mimetype, attachment.filename)
                                        }
                                    }
                                    setOnLongClickListener {
                                        holder.viewLongClicked()
                                        true
                                    }
                                }
                            }
                            findViewById<LinearLayout>(R.id.llHolder).addView(attachmentView)
                        }
                    }

                    findViewById<TextView>(R.id.tvPlayVideo).beVisibleIf(mimetype.startsWith("video/"))
                }
            }
        }
    }

    private fun launchViewIntent(uri: Uri, mimetype: String, filename: String) {
        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, mimetype.toLowerCase())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                mActivity.startActivity(this)
            } catch (e: ActivityNotFoundException) {
                val newMimetype = filename.getMimeType()
                if (newMimetype.isNotEmpty() && mimetype != newMimetype) {
                    launchViewIntent(uri, newMimetype, filename)
                } else {
                    mActivity.toast(R.string.no_app_found)
                }
            } catch (e: Exception) {
                mActivity.showErrorToast(e)
            }
        }
    }

    private fun setupDateTime(view: View, dateTime: DateAndTimeModel) {
        view.apply {
            findViewById<TextView>(R.id.tvDateTime).apply {
                text = dateTime.date.formatFullDateOrTime(context, false, false)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize - 5)
            }

            findViewById<ImageView>(R.id.ivSim).beVisibleIf(hasMultipleSIMCards)
            findViewById<TextView>(R.id.tvSimNumber).beVisibleIf(hasMultipleSIMCards)
            if (hasMultipleSIMCards) {
                Log.d("TAG_SIM", "setupDateTime: ${dateTime.simID}")
                if (dateTime.simID.equals("?")) {
                    findViewById<TextView>(R.id.tvSimNumber).text = "1"
                } else {
                    findViewById<TextView>(R.id.tvSimNumber).text = dateTime.simID
                }
            }
        }
    }

    private fun setupThreadSuccess(view: View, isDelivered: Boolean = false) {
        view.findViewById<ImageView>(R.id.tvMessageSuccess).setImageResource(if (isDelivered) R.drawable.icon_checked_double else R.drawable.icon_check)
    }

    private fun setupThreadError(view: View) {
        view.findViewById<TextView>(R.id.tvMessageError).setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize - 4)
    }

    private fun setupThreadSending(view: View) {
        view.findViewById<TextView>(R.id.tvSending).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
        }
    }

    interface DeleteLuancherListner {
        fun onDeleteLuancherCall(message: String, pos: Int)
    }
}
