package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.model.AutoCompressModel

class NotificationPreviewAdapter(
    private val list: List<AutoCompressModel>,
    private val previewName: String,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<NotificationPreviewAdapter.ViewHolder>() {
    private var selectedPos = RecyclerView.NO_POSITION

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.txtName)
        val imgChecked: ImageView = itemView.findViewById(R.id.imgChecked)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_list_language, viewGroup, false)
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.tvName.text = list[position].name

        holder.itemView.setOnClickListener {
            selectedPos = position
            onItemClick.invoke(list[position].name)
        }
        Log.d("TAG_PREVIEW_TYPE", "onBindViewHolder: $previewName")
        if (previewName == list[position].name && selectedPos == RecyclerView.NO_POSITION) {
            selectedPos = position
        }
        if (selectedPos == position) {
            holder.imgChecked.setImageResource(R.drawable.ic_checked)
        } else {
            holder.imgChecked.setImageResource(R.drawable.ic_unchecked)
        }
    }


    override fun getItemCount(): Int {
        return list.size
    }
}
