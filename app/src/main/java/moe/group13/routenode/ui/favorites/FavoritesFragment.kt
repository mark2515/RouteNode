package moe.group13.routenode.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = RouteAdapter(
            emptyList(),
            onClick = { route ->
                // originally toggle favorite
                onRouteClick(route)
            },
            onMenuClick = { route, view ->
                showOptionsMenu(route, view)
            }
        )
        recyclerView.adapter = adapter

        //TESTING
        val testRoutes = getSampleFavorites()
        adapter.update(testRoutes)
        updateEmptyState(testRoutes.isEmpty())

        // TODO: uncomment for testing
        // Observe favorite
        viewModel.favorites.observe(viewLifecycleOwner) { routes ->
            adapter.update(routes)
            updateEmptyState(routes.isEmpty())
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
        // Handle route click - could navigate to route details screen
        // For now, just toggle favorite as an example
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
        viewModel.toggleFavorite(route)
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorites when fragment becomes visible
        viewModel.loadFavorites()
    }

    private fun showOptionsMenu(route: Route, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_item_options, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    showEditDialog(route)
                    true
                }
                R.id.action_delete -> {
                    viewModel.deleteFavorite(route)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    private fun showEditDialog(route: Route){
        //TODO: Implement editing
    }

    private fun getSampleFavorites(): List<Route> {
        return listOf(
            Route(
                id = "route1",
                title = "Route A",
                description = "Description A",
                distanceKm = 3.1
            ),
            Route(
                id = "route2",
                title = "Route B",
                description = "Description B",
                distanceKm = 5.4
            ),
            Route(
                id = "route3",
                title = "Route C",
                description = "Description C",
                distanceKm = 2.8
            )
        )
    }



}