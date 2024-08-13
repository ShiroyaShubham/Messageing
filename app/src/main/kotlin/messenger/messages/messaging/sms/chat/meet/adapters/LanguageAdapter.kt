package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.utils.SharedPrefrenceClass
import messenger.messages.messaging.sms.chat.meet.model.LanguageModel
import messenger.messages.messaging.sms.chat.meet.utils.LANG_POS

class LanguageAdapter(
    private val langList: List<LanguageModel>,
  private val  itemClick: (Any) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {
    private var selectedPos = SharedPrefrenceClass.getInstance()?.getInt(LANG_POS,0)

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
        holder.tvName.text = langList[position].name
        holder.itemView.setOnClickListener {
            selectedPos = position
            itemClick.invoke(position)
            notifyDataSetChanged()
        }
        if (selectedPos == position) {
            holder.imgChecked.setImageResource(R.drawable.ic_checked)
        } else {
            holder.imgChecked.setImageResource(R.drawable.ic_unchecked)
        }
    }

    fun getSelectedLangPos(): Int {
        return selectedPos!!
    }

    override fun getItemCount(): Int {
        return langList.size
    }
}
