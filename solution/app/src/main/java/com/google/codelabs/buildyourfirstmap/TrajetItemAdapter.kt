package com.google.codelabs.buildyourfirstmap


import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrajetItemAdapter(private val mList: MutableList<Trajet>) : RecyclerView.Adapter<TrajetItemAdapter.ViewHolder>() {
    private var onClickListener: OnClickListener? = null
    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.trajets_item, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]

        // sets the text to the textview from our itemHolder class
        holder.departure.text = ItemsViewModel.departName
        holder.arrival.text = ItemsViewModel.arrivalName
        // Finally add an onclickListener to the item.
    holder.itemView.setOnClickListener {
        if (onClickListener != null) {
            onClickListener!!.onClick(position, ItemsViewModel )
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
        fun onClick(position: Int, model: Trajet)
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val departure: TextView = itemView.findViewById(R.id.departure_txt)
        val arrival: TextView = itemView.findViewById(R.id.arrival_txt)
    }

}