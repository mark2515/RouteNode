package moe.group13.routenode.ui.account

import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R

class SavedLocationAdapter(
    private val onItemClick: ((LocationItem) -> Unit)? = null
) : RecyclerView.Adapter<SavedLocationAdapter.LocationHolder>() {

    private var list: List<LocationItem> = emptyList()

    fun submitList(newList: List<LocationItem>) {
        list = newList
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): LocationItem = list[position]

    inner class LocationHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: LocationItem) {
            val nameTextView = itemView.findViewById<TextView>(R.id.txtLocationName)
            val addressTextView = itemView.findViewById<TextView>(R.id.txtLocationAddress)
            
            nameTextView.text = item.name
            addressTextView.text = item.address

            val isDarkTheme = (itemView.context.resources.configuration.uiMode and 
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            
            if (isDarkTheme) {
                nameTextView.setTextColor(Color.WHITE)
                addressTextView.setTextColor(Color.WHITE)
            } else {
                nameTextView.setTextColor(Color.parseColor("#000000"))
                addressTextView.setTextColor(Color.parseColor("#555555"))
            }

            // Optional: handle click (open map)
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
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

    override fun getItemCount(): Int = list.size
}
