package messenger.messages.messaging.sms.chat.meet.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import messenger.messages.messaging.sms.chat.meet.R
import messenger.messages.messaging.sms.chat.meet.send_message.Utils

class SubscriptionPlanAdapter(
    private val itemClick: (ProductDetails) -> Unit
) : RecyclerView.Adapter<SubscriptionPlanAdapter.ViewHolder>() {
    private var selectedPos = 0
    private var skuDetailsList: ArrayList<ProductDetails> = ArrayList()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.txtName)
        val tvPlan: TextView = itemView.findViewById(R.id.txtPlan)
        val tvPrice: TextView = itemView.findViewById(R.id.txtPrice)
        val imgChecked: ImageView = itemView.findViewById(R.id.imgChecked)
        val clMain: LinearLayout = itemView.findViewById(R.id.clMain)
    }


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_subscription_plan, viewGroup, false)
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item = skuDetailsList[position]
        when (position) {
            0 -> {
                holder.tvName.text = "Weekly"
                holder.tvPlan.text = "Auto-renewing subscription\nCharged weekly"
            }

            1 -> {
                holder.tvName.text = "Monthly"
                holder.tvPlan.text = "Auto-renewing subscription\nCharged Monthly"
            }

            2 -> {
                holder.tvName.text = "Yearly"
                holder.tvPlan.text = "Auto-renewing subscription\nCharged Yearly"
            }
        }
        holder.tvPrice.text = item.oneTimePurchaseOfferDetails?.formattedPrice

        holder.itemView.setOnClickListener {
            selectedPos = position
            itemClick.invoke(item)
            notifyDataSetChanged()
        }
        if (selectedPos == position) {
            if (Utils.isDarkMode(holder.itemView.context)) {
                holder.imgChecked.setColorFilter(Color.WHITE)
            }
            holder.clMain.setBackgroundResource(R.drawable.bg_blue_stroke_round_corner_15)
            holder.imgChecked.setImageResource(R.drawable.ic_checked)
        } else {
            holder.clMain.setBackgroundResource(R.drawable.bg_dotted_round_corner_15)
            holder.imgChecked.setImageResource(R.drawable.ic_unchecked)
        }
    }


    fun updateList(skuDetailsList: MutableList<ProductDetails>) {
        this.skuDetailsList = skuDetailsList as ArrayList<ProductDetails>
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
        return skuDetailsList.size
    }
}
