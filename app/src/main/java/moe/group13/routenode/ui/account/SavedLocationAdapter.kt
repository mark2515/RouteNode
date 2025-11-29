package moe.group13.routenode.ui.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.ui.account.LocationItem
import moe.group13.routenode.R

class SavedLocationAdapter :
    RecyclerView.Adapter<SavedLocationAdapter.LocationHolder>() {

    private var list: List<LocationItem> = emptyList()

    fun submitList(newList: List<LocationItem>) {
        list = newList
        notifyDataSetChanged()
    }

    class LocationHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: LocationItem) {
            itemView.findViewById<TextView>(R.id.txtLocationName).text = item.name
            itemView.findViewById<TextView>(R.id.txtLocationAddress).text = item.address
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_location, parent, false)
        return LocationHolder(view)
    }

    override fun onBindViewHolder(holder: LocationHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size
}