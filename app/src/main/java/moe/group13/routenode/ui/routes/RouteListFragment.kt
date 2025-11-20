package moe.group13.routenode.ui.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R

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
                // Navigate to route details
                navigateToRouteDetails(route)
            },
            onFavoriteClick = { route ->
                viewModel.toggleFavorite(route)
            },
            isFavorite = { routeId ->
                // Check if route is in favorites
                viewModel.favorites.value?.any { it.id == routeId } ?: false
            }
        )
        recyclerView.adapter = adapter

        viewModel.publicRoutes.observe(viewLifecycleOwner) {
            adapter.update(it)
        }
        
        viewModel.favorites.observe(viewLifecycleOwner) {
            // Refresh adapter when favorites change to update favorite button states
            adapter.notifyDataSetChanged()
        }

        viewModel.loadPublicRoutes()
        viewModel.loadFavorites()
    }
    
    private fun navigateToRouteDetails(route: moe.group13.routenode.data.model.Route) {
        val intent = android.content.Intent(requireContext(), moe.group13.routenode.ui.routes.RouteDetailsActivity::class.java)
        intent.putExtra("route_id", route.id)
        intent.putExtra("route_title", route.title)
        intent.putExtra("route_description", route.description)
        intent.putExtra("route_distance", route.distanceKm)
        startActivity(intent)
    }
}
