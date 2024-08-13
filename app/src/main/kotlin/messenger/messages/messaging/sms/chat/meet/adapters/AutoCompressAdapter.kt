package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.model.AutoCompressModel

class AutoCompressAdapter(
    private val list: List<AutoCompressModel>,
    private val sizeLimit: Long,
) : RecyclerView.Adapter<AutoCompressAdapter.ViewHolder>() {
    private var selectedPos = -1

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
            notifyDataSetChanged()
        }
        if(sizeLimit == list[position].size && selectedPos == -1){
            selectedPos = position
        }
        if (selectedPos == position) {
            holder.imgChecked.setImageResource(R.drawable.ic_checked)
        } else {
            holder.imgChecked.setImageResource(R.drawable.ic_unchecked)
        }
    }

    fun getSelectedPos(): Int {
        return selectedPos
    }

    override fun getItemCount(): Int {
        return list.size
    }
}
