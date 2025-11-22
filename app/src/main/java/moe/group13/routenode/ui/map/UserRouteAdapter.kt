package moe.group13.routenode.ui.map


import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import moe.group13.routenode.R

class UserRouteAdapter(
    private val userPins: List<UserPin>,
    private val onClick: (UserPin) -> Unit
) : RecyclerView.Adapter<UserRouteAdapter.UserRouteViewHolder>() {

    class UserRouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.route_user_name)
        val stops: TextView = itemView.findViewById(R.id.route_stops)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserRouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_map_user_routes, parent, false)
        return UserRouteViewHolder(view)
    }

    override fun getItemCount() = userPins.size

    override fun onBindViewHolder(holder: UserRouteViewHolder, position: Int) {
        val pin = userPins[position]
        holder.userName.text = pin.userName
        holder.stops.text = pin.destination.mapIndexed { i, stop ->
            "Stop ${i + 1}: $stop"
        }.joinToString("\n")


        holder.itemView.setOnClickListener { onClick(pin) }
    }
}
