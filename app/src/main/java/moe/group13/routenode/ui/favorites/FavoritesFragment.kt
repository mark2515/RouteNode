package moe.group13.routenode.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.ui.map.MapActivity
import moe.group13.routenode.ui.routes.RouteAdapter
import moe.group13.routenode.ui.routes.RouteViewModel

class FavoritesFragment : Fragment() {
    private val viewModel: RouteViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: View
    private lateinit var progressBar: View
    private lateinit var adapter: RouteAdapter
    private lateinit var searchEditText: TextInputEditText
    private var allFavorites: List<Route> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.favoritesRecyclerView)
        emptyText = view.findViewById(R.id.emptyFavoritesText)
        progressBar = view.findViewById(R.id.favoritesProgressBar)
        searchEditText = view.findViewById(R.id.searchEditText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = RouteAdapter(
            emptyList(),
            onClick = { route ->
                onRouteClick(route)
            },
            onMenuClick = { route, view ->
                showOptionsMenu(route, view)
            },
            onFavoriteClick = { route ->
                showRemoveFavoriteDialog(route)
            },
            isFavoriteCheck = { routeId, callback ->
                viewModel.isFavorite(routeId, callback)
            }
        )
        recyclerView.adapter = adapter

        // Setup search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterFavorites(s?.toString() ?: "")
            }
        })

        /*
        //TESTING
        val testRoutes = getSampleFavorites()
        adapter.update(testRoutes)
        updateEmptyState(testRoutes.isEmpty())
        */
        viewModel.favorites.observe(viewLifecycleOwner) { routes ->
            allFavorites = routes
            filterFavorites(searchEditText.text?.toString() ?: "")
        }
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Could show a Snackbar or Toast here
                android.util.Log.e("FavoritesFragment", it)
            }
        }

        // Load favorites when fragment is created
        viewModel.loadFavorites()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val isLoading = viewModel.isLoading.value ?: false
        if (isEmpty && !isLoading) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun onRouteClick(route: Route) {
        // Handle route click - navigate to map with route
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Start Route")
            .setMessage("Do you want to go on this route: ${route.title}?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(requireContext(), MapActivity::class.java).apply {
                    putExtra("EXTRA_ROUTE_NAME", route.title)
                    //Vancouver
                    putExtra("latitude", 49.2827)
                    putExtra("longitude", -123.1207)
                }
                startActivity(intent)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorites when fragment becomes visible
        viewModel.loadFavorites()
    }

    private fun showOptionsMenu(route: Route, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_item_options, popup.menu)

        // Hide menu items that don't apply to favorites
        popup.menu.findItem(R.id.action_add_favorite)?.isVisible = false
        popup.menu.findItem(R.id.action_delete)?.isVisible = false

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_remove_favorite -> {
                    showRemoveFavoriteDialog(route)
                    true
                }
                R.id.action_edit -> {
                    showEditDialog(route)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    private fun filterFavorites(query: String) {
        val filtered = if (query.isBlank()) {
            allFavorites
        } else {
            val lowerQuery = query.lowercase()
            allFavorites.filter { route ->
                route.title.lowercase().contains(lowerQuery) ||
                route.description.lowercase().contains(lowerQuery) ||
                // Search in route node data if available
                (route.routeNodeDataJson.lowercase().contains(lowerQuery))
            }
        }
        adapter.update(filtered)
        updateEmptyState(filtered.isEmpty() && allFavorites.isNotEmpty())
    }

    private fun showEditDialog(route: Route){
        // Check if route has node data for editing
        if (route.routeNodeDataJson.isBlank()) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cannot Edit")
                .setMessage("This favorite doesn't have editable route data. Only favorites saved from the search page can be edited.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        // Navigate to SearchFragment with route data
        val mainActivity = activity as? moe.group13.routenode.MainActivity
        mainActivity?.let {
            // Switch to SearchFragment (index 0)
            it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.view_pager)?.currentItem = 0
            // Pass route data via shared preferences
            val prefs = requireContext().getSharedPreferences("route_edit", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("edit_route_id", route.id)
                putString("edit_route_title", route.title)
                putString("edit_route_description", route.description)
                putString("edit_route_node_data", route.routeNodeDataJson)
                putFloat("edit_route_distance", route.distanceKm.toFloat())
                apply()
            }
        }
    }

    private fun showRemoveFavoriteDialog(route: Route) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Remove Favorite")
            .setMessage("Are you sure you want to remove '${route.title}' from favorites?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeFavorite(route.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}