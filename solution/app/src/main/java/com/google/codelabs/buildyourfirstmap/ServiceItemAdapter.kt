package com.google.codelabs.buildyourfirstmap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ServiceItemAdapter(private val mList: MutableList<Service>,val tList : List<Trajet>) : RecyclerView.Adapter<ServiceItemAdapter.ViewHolder>() {
    private var onClickListener: OnClickListener? = null

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_service_list, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]
        var arrival = tList[ItemsViewModel.trajet]
        var arrivaltxt = ""
        if(arrival.departName == ItemsViewModel.depart){arrivaltxt = arrival.arrivalName}
        else{ arrivaltxt = arrival.departName}



        if(ItemsViewModel.status == "Moving")
        {
            // sets the text to the textview from our itemHolder class
            holder.arrival_txt.text = arrivaltxt
            holder.txt_time.text = "Bus has left since : " +ItemsViewModel.hours
            holder.busOrNone.setBackgroundResource(R.drawable.ic_moving_bus)
            holder.busStatus.setBackgroundResource(R.drawable.ic_right_arrow)
        }
        else
        {
            // sets the text to the textview from our itemHolder class
            holder.arrival_txt.text = arrival.departName
            holder.txt_time.text = "Bus is at Bus Park since : " +ItemsViewModel.hours
            holder.busOrNone.setBackgroundResource(R.drawable.ic_buspark)
            holder.busStatus.setBackgroundResource(R.drawable.ic_right_arrow)
        }
        // Finally add an onclickListener to the item.
        holder.itemView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(position, ItemsViewModel)
            }
        }
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {

        return mList.size
    }

    // A function to bind the onclickListener.
    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    // onClickListener Interface
    interface OnClickListener {
        fun onClick(position: Int, model: Service)
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val busOrNone = itemView.findViewById<ImageView>(R.id.img_busOrNone)
        val busStatus = itemView.findViewById<ImageView>(R.id.img_busStatus)
        val arrival_txt = itemView.findViewById<TextView>(R.id.arrival_txt)
        val txt_time = itemView.findViewById<TextView>(R.id.txt_time)
    }
}
