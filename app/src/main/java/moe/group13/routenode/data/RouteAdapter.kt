package moe.group13.routenode.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R
import moe.group13.routenode.data.Route

class RouteAdapter(
    private var routes: List<Route>,
    private val onClick: (Route) -> Unit
): RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {
    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.route_title)
        val description: TextView = itemView.findViewById(R.id.route_description)
        val distance: TextView = itemView.findViewById(R.id.route_distance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]
        holder.title.text = route.title
        holder.description.text = route.description
        holder.distance.text = "${route.distanceKm} km"

        holder.itemView.setOnClickListener {
            onClick(route)
        }
    }

    override fun getItemCount(): Int = routes.size

    fun update(newRoutes: List<Route>) {
        routes = newRoutes
        notifyDataSetChanged()
    }
}