package moe.group13.routenode.ui.routes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route

class RouteAdapter(
    private var routes: List<Route>,
    private val onClick: (Route) -> Unit,
    private val onFavoriteClick: ((Route) -> Unit)? = null,
    private val isFavorite: ((String) -> Boolean)? = null
): RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {
    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.route_title)
        val description: TextView = itemView.findViewById(R.id.route_description)
        val distance: TextView = itemView.findViewById(R.id.route_distance)
        val favoriteButton: ImageButton = itemView.findViewById(R.id.buttonFavorite)
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

        // Update favorite button state
        val favorited = isFavorite?.invoke(route.id) ?: false
        holder.favoriteButton.setImageResource(
            if (favorited) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
        holder.favoriteButton.contentDescription = if (favorited) "Remove from favorites" else "Add to favorites"

        // Set click listeners
        holder.itemView.setOnClickListener {
            onClick(route)
        }

        holder.favoriteButton.setOnClickListener {
            onFavoriteClick?.invoke(route)
            // Update UI immediately for better UX
            val newFavorited = !favorited
            holder.favoriteButton.setImageResource(
                if (newFavorited) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
        }
    }

    override fun getItemCount(): Int = routes.size

    fun update(newRoutes: List<Route>) {
        routes = newRoutes
        notifyDataSetChanged()
    }
}