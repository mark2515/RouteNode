package moe.group13.routenode.ui.routes

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route

class RouteListFragment : Fragment() {
    private val viewModel: RouteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_route_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.routeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter = RouteAdapter(
            emptyList(),
            onClick = { route ->
                // Handle route click - could navigate to route details
                Toast.makeText(requireContext(), "Route: ${route.title}", Toast.LENGTH_SHORT).show()
            },
            onMenuClick = { route, anchorView ->
                showOptionsMenu(route, anchorView)
            },
            onFavoriteClick = { route ->
                viewModel.toggleFavorite(route)
            },
            isFavoriteCheck = { routeId, callback ->
                viewModel.isFavorite(routeId, callback)
            }
        )
        recyclerView.adapter = adapter

        viewModel.publicRoutes.observe(viewLifecycleOwner) {
            adapter.update(it)
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadPublicRoutes()
    }
    
    private fun showOptionsMenu(route: Route, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_item_options, popup.menu)
        
        // Show/hide menu items based on favorite status
        viewModel.isFavorite(route.id) { isFavorite ->
            popup.menu.findItem(R.id.action_add_favorite)?.isVisible = !isFavorite
            popup.menu.findItem(R.id.action_remove_favorite)?.isVisible = isFavorite
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add_favorite -> {
                    showFavoriteNameDialog(route)
                    true
                }
                R.id.action_remove_favorite -> {
                    viewModel.removeFavorite(route.id)
                    Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_edit -> {
                    // TODO: Implement editing
                    Toast.makeText(requireContext(), "Edit route", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_delete -> {
                    // TODO: Implement deletion (only for own routes)
                    Toast.makeText(requireContext(), "Delete route", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    
    private fun showFavoriteNameDialog(route: Route) {
        // Get current favorite count for default name
        val currentFavorites = viewModel.favorites.value ?: emptyList()
        val defaultName = "favorite-${currentFavorites.size + 1}"
        
        // Show dialog to name the favorite
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(defaultName)
        input.selectAll()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Name Your Favorite")
            .setMessage("Enter a name for this favorite route:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val favoriteName = input.text.toString().trim()
                if (favoriteName.isBlank()) {
                    Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.saveFavorite(route, favoriteName)
                Toast.makeText(requireContext(), "Added to favorites", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
