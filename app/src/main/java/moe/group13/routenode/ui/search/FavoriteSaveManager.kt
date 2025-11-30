package moe.group13.routenode.ui.search

import android.content.Context
import android.location.Geocoder
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson
import kotlinx.coroutines.launch
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.ui.routes.RouteViewModel
import java.util.Locale

class FavoriteSaveManager(
    private val fragment: Fragment,
    private val routeViewModel: RouteViewModel,
    private val routeNodeAdapter: RouteNodeAdapter,
    private val viewModel: SearchViewModel
) {

    fun saveRouteAsFavorite(isEditMode: Boolean) {
        if (isEditMode) {
            Toast.makeText(
                fragment.requireContext(),
                "Please use 'Done' button to save changes",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val routeNodeData = routeNodeAdapter.getRouteNodeData()
        val aiResponse = viewModel.aiResponse.value

        if (routeNodeData.isEmpty()) {
            Toast.makeText(fragment.requireContext(), "No route data to save", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current favorite count for default name
        fragment.lifecycleScope.launch {
            val defaultName = routeViewModel.getNextDefaultFavoriteName()

            // Show dialog to name the favorite
            val input = EditText(fragment.requireContext())
            input.inputType = InputType.TYPE_CLASS_TEXT
            input.setText(defaultName)
            input.selectAll()

            AlertDialog.Builder(fragment.requireContext())
                .setTitle("Name Your Favorite")
                .setMessage("Enter a name for this favorite route:")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val favoriteName = input.text.toString().trim()
                    if (favoriteName.isBlank()) {
                        Toast.makeText(fragment.requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    saveFavoriteWithName(routeNodeData, aiResponse, favoriteName)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun saveFavoriteWithName(
        routeNodeData: List<RouteNodeAdapter.RouteNodeData>,
        aiResponse: String?,
        favoriteName: String
    ) {
        Log.d("FavoriteSaveManager", "AI Response: $aiResponse")
        
        val regex = Regex("```(.*?)```", RegexOption.DOT_MATCHES_ALL)
        val waypoints = regex.findAll(aiResponse ?: "")
            .map { match: MatchResult -> match.groups[1]?.value?.trim() ?: "" }
            .toList()

        Log.d("FavoriteSaveManager", "Waypoints: $waypoints")
        
        // Convert addresses to geocode
        val geoPoints = convertAddressesToGeoPoints(fragment.requireContext(), waypoints)
        
        // Calculate total distance
        val totalDistance = routeNodeData.sumOf {
            it.distance.toDoubleOrNull() ?: 0.0
        }

        // Create route description from AI response or route nodes
        val routeDescription = aiResponse ?: run {
            routeNodeData.joinToString("\n") { node ->
                "Node ${node.no}: ${node.location} - ${node.place} (${node.distance} km)"
            }
        }

        // Generate a unique ID for the route
        val routeId = java.util.UUID.randomUUID().toString()

        // Get current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        val creatorId = currentUser?.uid ?: ""

        if (creatorId.isEmpty()) {
            Toast.makeText(fragment.requireContext(), "Please log in to save favorites", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert route node data to JSON
        val gson = Gson()
        val routeNodeDataJson = gson.toJson(routeNodeData)

        // Create a Route object
        val route = Route(
            id = routeId,
            title = favoriteName,
            description = routeDescription,
            waypoints = geoPoints,
            distanceKm = totalDistance,
            creatorId = creatorId,
            isPublic = false,
            tags = waypoints,
            estimatedDurationMinutes = (totalDistance * 12).toInt(), // Rough estimate: 12 min/km for walking
            difficulty = "easy",
            rating = 0.0,
            ratingCount = 0,
            favoriteCount = 0,
            imageUrl = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            routeNodeDataJson = routeNodeDataJson
        )

        // Save as favorite
        routeViewModel.saveFavorite(route, favoriteName)
        Toast.makeText(fragment.requireContext(), "Route saved to favorites!", Toast.LENGTH_SHORT).show()

        // Reset the input screen after saving
        routeNodeAdapter.reset()
        viewModel.clearAiResponse()
    }
    //use google geocoder to convert addresses to geopoints, for google maps pins
    private fun convertAddressesToGeoPoints(
        context: Context,
        addresses: List<String>
    ): List<GeoPoint> {
        val geoPoints = mutableListOf<GeoPoint>()
        val geocoder = Geocoder(context, Locale.getDefault())
        for (address in addresses) {
            try {
                val results = geocoder.getFromLocationName(address, 1)
                if (!results.isNullOrEmpty()) {
                    val location = results[0]
                    val currentGeoPoint = GeoPoint(location.latitude, location.longitude)
                    geoPoints.add(currentGeoPoint)
                } else {
                    Log.d("FavoriteSaveManager", "No location found for address: $address")
                }
            } catch (e: Exception) {
                Log.d("FavoriteSaveManager", "Error geocoding address: $address", e)
            }
        }
        return geoPoints
    }
}