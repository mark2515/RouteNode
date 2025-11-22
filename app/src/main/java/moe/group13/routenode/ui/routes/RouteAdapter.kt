package moe.group13.routenode.ui.routes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route

class RouteAdapter(
    private var routes: List<Route>,
    private val onClick: (Route) -> Unit,
    private val onMenuClick: (Route, View) -> Unit,
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
        //routename: Location and Place looking for
        holder.title.text = route.title
        holder.description.text = route.description
        holder.distance.text = "${route.distanceKm} km"

        //open dialog and on confirm, opens map and directions
        holder.itemView.setOnClickListener {
            onClick(route)
        }
        //edit/delete menu
        val moreBtn = holder.itemView.findViewById<View>(R.id.iv_more)
        moreBtn.setOnClickListener {
            onMenuClick(route, it)
        }

    }

    override fun getItemCount(): Int = routes.size

    fun update(newRoutes: List<Route>) {
        routes = newRoutes
        notifyDataSetChanged()
    }
}