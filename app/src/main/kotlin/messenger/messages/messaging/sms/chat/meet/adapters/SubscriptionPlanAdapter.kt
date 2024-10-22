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
import messenger.messages.messaging.sms.chat.meet.utils.Utility

class SubscriptionPlanAdapter(
    private val itemClick: (ProductDetails) -> Unit
) : RecyclerView.Adapter<SubscriptionPlanAdapter.ViewHolder>() {
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
        holder.tvName.text = item.name
        holder.tvPlan.text = "Auto-renewing subscription\nCharged ${item.name}"
        holder.tvPrice.text = item.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(
            0
        )?.formattedPrice
            ?: ""

        holder.itemView.setOnClickListener {
            Utility.selectedPos = position
            itemClick.invoke(item)
            notifyDataSetChanged()
        }

        if (Utility.selectedPos == position) {
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


    fun updateList(skuDetailsList: List<ProductDetails>) {
      //  this.skuDetailsList = skuDetailsList as ArrayList<ProductDetails>
        this.skuDetailsList.clear()
        this.skuDetailsList.addAll(skuDetailsList)
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
        return skuDetailsList.size
    }
}
