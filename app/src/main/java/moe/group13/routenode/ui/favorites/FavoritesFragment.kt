package moe.group13.routenode.ui.favorites


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import moe.group13.routenode.R
import moe.group13.routenode.ui.manual.AppDatabase
import moe.group13.routenode.ui.manual.RestaurantDao
import moe.group13.routenode.ui.manual.RouteDao

class FavoritesFragment : Fragment() {
    private lateinit var db: AppDatabase
    private lateinit var routeDao: RouteDao
    private lateinit var restaurantDao: RestaurantDao
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /*
        // Initialize Room database and DAOs
        db = AppDatabase.getDatabase(requireContext())
        routeDao = db.routeDao()
        restaurantDao = db.restaurantDao()

    */
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }
/*
    override fun onResume() {
        super.onResume()
        loadRoutes()
    }
    private fun loadRoutes() {
        val containerLayout = view?.findViewById<LinearLayout>(R.id.historyContainer)
        containerLayout?.removeAllViews()
        lifecycleScope.launch {
            val routes = routeDao.getAllRoutes()
            routes.forEach { route ->
                val routeTv = TextView(requireContext())
                routeTv.text = "Route: ${route.name}"
                routeTv.textSize = 18f
                routeTv.setPadding(16, 16, 16, 8)
                containerLayout?.addView(routeTv)

                // Fetch restaurants for this route
                val restaurants = restaurantDao.getRestaurantsForRoute(route.routeId)
                restaurants.forEach { restaurant ->
                    val restaurantTv = TextView(requireContext())
                    restaurantTv.text = "  - ${restaurant.name} (${restaurant.lat}, ${restaurant.lng})"
                    restaurantTv.setPadding(32, 8, 16, 8)
                    containerLayout?.addView(restaurantTv)
                }
            }
        }
    }

 */
}