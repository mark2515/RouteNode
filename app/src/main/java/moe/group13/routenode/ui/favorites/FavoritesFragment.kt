package moe.group13.routenode.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route
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
                // Handle route click - navigate to route details
                onRouteClick(route)
            },
            onFavoriteClick = { route ->
                // Remove from favorites when favorite button clicked
                viewModel.toggleFavorite(route)
            },
            isFavorite = { routeId ->
                // All routes in favorites fragment are favorited
                true
            }
        )
        recyclerView.adapter = adapter

        // Observe favorites
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
        try {
            viewModel.loadFavorites()
        } catch (e: Exception) {
            android.util.Log.e("FavoritesFragment", "Error loading favorites on create", e)
        }
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
        // Navigate to route details screen
        val intent = android.content.Intent(requireContext(), moe.group13.routenode.ui.routes.RouteDetailsActivity::class.java)
        intent.putExtra("route_id", route.id)
        intent.putExtra("route_title", route.title)
        intent.putExtra("route_description", route.description)
        intent.putExtra("route_distance", route.distanceKm)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorites when fragment becomes visible
        viewModel.loadFavorites()
    }
}