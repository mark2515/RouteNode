package moe.group13.routenode.ui.routes

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route

class RouteAdapter(
    private var routes: List<Route>,
    private val onClick: (Route) -> Unit,
    private val onMenuClick: (Route, View) -> Unit,
    private val onFavoriteClick: ((Route) -> Unit)? = null,
    private val isFavoriteCheck: ((String, (Boolean) -> Unit) -> Unit)? = null,
    private val truncateDescription: Boolean = false // Whether to truncate description to 2-3 sentences
): RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {
    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.route_title)
        val description: TextView = itemView.findViewById(R.id.route_description)
        val distance: TextView = itemView.findViewById(R.id.route_distance)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.iv_favorite)
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
        
        // Truncate description to 2-3 sentences if needed
        val displayDescription = if (truncateDescription && route.description.isNotBlank()) {
            truncateToSentences(route.description, maxSentences = 3)
        } else {
            route.description
        }
        holder.description.text = displayDescription
        
        holder.distance.text = "${route.distanceKm} km"

        // Update favorite icon state
        if (isFavoriteCheck != null && onFavoriteClick != null) {
            isFavoriteCheck(route.id) { isFavorite ->
                updateFavoriteIcon(holder.favoriteIcon, isFavorite)
            }
            
            holder.favoriteIcon.setOnClickListener {
                onFavoriteClick(route)
                // Optimistically update UI
                isFavoriteCheck(route.id) { isFavorite ->
                    updateFavoriteIcon(holder.favoriteIcon, !isFavorite)
                }
            }
        } else {
            // Hide favorite icon if callbacks not provided
            holder.favoriteIcon.visibility = View.GONE
        }

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

    private fun updateFavoriteIcon(icon: ImageView, isFavorite: Boolean) {
        if (isFavorite) {
            icon.setColorFilter(Color.parseColor("#FFD700")) // Gold color for favorited
            icon.alpha = 1.0f
        } else {
            icon.setColorFilter(Color.parseColor("#808080")) // Gray color for not favorited
            icon.alpha = 0.6f
        }
    }

    override fun getItemCount(): Int = routes.size

    fun update(newRoutes: List<Route>) {
        routes = newRoutes
        notifyDataSetChanged()
    }
    
    private fun truncateToSentences(text: String, maxSentences: Int = 3): String {
        if (text.isBlank()) {
            return text
        }
        
        // Split by sentence endings (. ! ?) followed by space or end of string
        val sentences = text.split(Regex("(?<=[.!?])(?=\\s|$)"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        if (sentences.size <= maxSentences) {
            return text
        }
        
        // Take first maxSentences sentences
        val truncated = sentences.take(maxSentences).joinToString(" ")
        
        // Add "...more" at the end
        return "$truncated...more"
    }
}