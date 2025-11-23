package moe.group13.routenode.ui.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
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
                    viewModel.saveFavorite(route)
                    Toast.makeText(requireContext(), "Added to favorites", Toast.LENGTH_SHORT).show()
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
}
