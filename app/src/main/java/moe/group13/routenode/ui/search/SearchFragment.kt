package moe.group13.routenode.ui.search

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.ui.routes.RouteViewModel
import moe.group13.routenode.utils.GptConfig

class SearchFragment : Fragment() {
    
    private lateinit var viewModel: SearchViewModel
    private val routeViewModel: RouteViewModel by viewModels()
    private lateinit var placesClient: PlacesClient
    private lateinit var routeNodeAdapter: RouteNodeAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        
        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(requireContext())
        
        // Setup RecyclerView
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerRouteNodes)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        
        // Setup Ask AI button
        val buttonAskAI = view.findViewById<MaterialButton>(R.id.buttonAskAI)
        buttonAskAI.setOnClickListener {
            // Check if all fields are valid
            if (::routeNodeAdapter.isInitialized && !routeNodeAdapter.isAllFieldsValid()) {
                // Show validation errors
                routeNodeAdapter.showAllValidationErrors()
                Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            } else {
                // Scroll to the bottom of the RecyclerView
                recycler.post {
                    recycler.smoothScrollToPosition(routeNodeAdapter.itemCount - 1)
                }
                askAIForAdvice()
            }
        }
        
        routeNodeAdapter = RouteNodeAdapter(
            mutableListOf(RouteNodeAdapter.RouteNodeData(no = 1)),
            placesClient,
            onRetryAi = {
                askAIForAdvice()
            },
            onValidationChanged = { isValid ->
            },
            onFavoriteAi = {
                saveRouteAsFavorite()
            }
        )
        recycler.adapter = routeNodeAdapter
        
        // Observe ViewModel
        observeViewModel()
        
        // Check if we need to load route data for editing
        checkForEditRoute()
    }
    
    private fun checkForEditRoute() {
        val prefs = requireContext().getSharedPreferences("route_edit", Context.MODE_PRIVATE)
        val routeNodeDataJson = prefs.getString("edit_route_node_data", null)
        if (routeNodeDataJson != null && routeNodeDataJson.isNotEmpty()) {
            try {
                val gson = Gson()
                val routeNodeData = gson.fromJson(routeNodeDataJson, Array<RouteNodeAdapter.RouteNodeData>::class.java).toList()
                if (routeNodeData.isNotEmpty() && ::routeNodeAdapter.isInitialized) {
                    routeNodeAdapter.setRouteNodeData(routeNodeData)
                    // Clear the edit flag
                    prefs.edit().remove("edit_route_node_data").apply()
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchFragment", "Error loading route data", e)
            }
        }
    }
    
    private fun askAIForAdvice() {
        // Get the current route node data from the adapter
        val routeNodeData = routeNodeAdapter.getRouteNodeData()

        // Read AI settings saved from AIModelsActivity
        val aiPrefs = requireContext().getSharedPreferences("ai_model_settings", Context.MODE_PRIVATE)
        val defaultConfig = GptConfig.DEFAULT_CONFIG

        val model = aiPrefs.getString("model", defaultConfig.model)
        val temperature = aiPrefs.getFloat(
            "temperature",
            defaultConfig.temperature.toFloat()
        ).toDouble()
        val maxTokens = aiPrefs.getInt(
            "max_tokens",
            defaultConfig.max_tokens
        )
        val responseLanguage = aiPrefs.getString("language", "English") ?: "English"

        // Read distance unit preference from settings
        val settingsPrefs = requireContext().getSharedPreferences("route_settings", Context.MODE_PRIVATE)
        val unitIndex = settingsPrefs.getInt("unit_index", 0)
        val distanceUnit = if (unitIndex == 1) "mi" else "km"

        // Call ViewModel with the actual user input data and AI settings
        viewModel.askAIForAdviceWithRouteNodes(
            routeNodeData = routeNodeData,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            distanceUnit = distanceUnit,
            responseLanguage = responseLanguage
        )
    }
    
    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val button = view?.findViewById<MaterialButton>(R.id.buttonAskAI)
            // Only disable during loading
            button?.isEnabled = !isLoading
            button?.text = if (isLoading) "Loading..." else "Ask AI for advice !"
            
            // Update loading spinner in adapter
            routeNodeAdapter.setLoadingState(isLoading)
        }
        
        // Observe AI response
        viewModel.aiResponse.observe(viewLifecycleOwner) { response ->
            routeNodeAdapter.setAiResponse(response)
            // Scroll to bottom when AI response is received
            view?.findViewById<RecyclerView>(R.id.recyclerRouteNodes)?.post {
                val recycler = view?.findViewById<RecyclerView>(R.id.recyclerRouteNodes)
                recycler?.smoothScrollToPosition(routeNodeAdapter.itemCount - 1)
            }
        }
        
        // Observe errors
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        
        // Observe route view model errors
        routeViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun saveRouteAsFavorite() {
        val routeNodeData = routeNodeAdapter.getRouteNodeData()
        val aiResponse = viewModel.aiResponse.value
        
        if (routeNodeData.isEmpty()) {
            Toast.makeText(requireContext(), "No route data to save", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get current favorite count for default name
        val currentFavorites = routeViewModel.favorites.value ?: emptyList()
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
                saveFavoriteWithName(routeNodeData, aiResponse, favoriteName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun saveFavoriteWithName(
        routeNodeData: List<RouteNodeAdapter.RouteNodeData>,
        aiResponse: String?,
        favoriteName: String
    ) {
        // Calculate total distance
        val totalDistance = routeNodeData.sumOf { 
            it.distance.toDoubleOrNull() ?: 0.0 
        }
        
        // Create route description from AI response or route nodes
        val routeDescription = aiResponse?.take(200) ?: run {
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
            Toast.makeText(requireContext(), "Please log in to save favorites", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Convert route node data to JSON
        val gson = Gson()
        val routeNodeDataJson = gson.toJson(routeNodeData)
        
        // Create a Route object with creatorId set
        val route = Route(
            id = routeId,
            title = favoriteName,
            description = routeDescription,
            waypoints = emptyList(), // Could be populated from locations if needed
            distanceKm = totalDistance,
            creatorId = creatorId, // Set to current user who is saving the favorite
            isPublic = false,
            tags = emptyList(),
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
        
        // Save as favorite with custom name
        routeViewModel.saveFavorite(route, favoriteName)
        Toast.makeText(requireContext(), "Route saved to favorites!", Toast.LENGTH_SHORT).show()
        
        // Reset the input screen after saving
        routeNodeAdapter.reset()
        // Clear AI response
        viewModel.clearAiResponse()
    }
    
    override fun onResume() {
        super.onResume()
        // Update distance units when returning from settings
        if (::routeNodeAdapter.isInitialized) {
            routeNodeAdapter.updateDistanceUnits()
        }
        // Check for edit route data
        checkForEditRoute()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        if (::routeNodeAdapter.isInitialized) {
            routeNodeAdapter.cleanup()
        }
    }
}