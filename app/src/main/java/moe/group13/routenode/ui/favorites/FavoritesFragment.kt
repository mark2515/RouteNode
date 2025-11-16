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
        adapter = RouteAdapter(emptyList()) { route ->
            // Handle route click - could navigate to route details
            onRouteClick(route)
        }
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
        viewModel.toggleFavorite(route)
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorites when fragment becomes visible
        viewModel.loadFavorites()
    }
}