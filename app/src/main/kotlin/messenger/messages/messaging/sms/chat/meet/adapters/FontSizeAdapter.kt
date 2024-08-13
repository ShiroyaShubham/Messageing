package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.model.ImageWithTextModel

class FontSizeAdapter(
    private val list: List<ImageWithTextModel>,
    fontSize: Int,
) : RecyclerView.Adapter<FontSizeAdapter.ViewHolder>() {
    private var selectedPos = fontSize

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.txtName)
        val imgChecked: ImageView = itemView.findViewById(R.id.imgChecked)
        val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
    }


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_list_language, viewGroup, false)
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.imgIcon.isVisible = true
        holder.tvName.text = list[position].name
        holder.imgIcon.setImageResource(list[position].icon)
        holder.itemView.setOnClickListener {
            selectedPos = position
            notifyDataSetChanged()
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
