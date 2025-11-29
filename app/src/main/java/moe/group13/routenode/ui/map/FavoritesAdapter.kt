package moe.group13.routenode.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route

class FavoritesAdapter(
    private val favorites: List<Route>,
    private val onGoClick: (Route) -> Unit,
    private val onPreviewClick: (Route) -> Unit,
    private val onEditClick: (Route) -> Unit
) :
    RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    class FavoriteViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.favorite_title)
        val goButton: Button = view.findViewById(R.id.go_button)

        val previewButton: Button = view.findViewById(R.id.preview_button)
        val editButton: Button = view.findViewById(R.id.edit_button)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_map_user_favorite, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun getItemCount(): Int {
        return favorites.size
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val favorite = favorites[position]
        holder.title.text = favorite.title

        holder.goButton.setOnClickListener {
            onGoClick(favorite)
        }
        holder.previewButton.setOnClickListener {
            onPreviewClick(favorite)
        }
        holder.editButton.setOnClickListener {
            onEditClick(favorite)
        }

    }
}