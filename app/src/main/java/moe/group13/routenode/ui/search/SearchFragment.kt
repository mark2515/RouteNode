package moe.group13.routenode.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.ui.RouteNodeAdapter
import moe.group13.routenode.ui.routes.RouteViewModel

class SearchFragment : Fragment() {
    
    private lateinit var viewModel: SearchViewModel
    private val routeViewModel: RouteViewModel by viewModels()
    private var placesClient: PlacesClient? = null
    private lateinit var adapter: RouteNodeAdapter
    
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
        // Note: This requires a Google Maps API key with Places API enabled
        // The key should be in res/values/google_maps_api.xml (not the Firebase key from google-services.json)
        try {
            val apiKey = getString(R.string.google_maps_key)
            android.util.Log.d("SearchFragment", "Attempting to initialize Places SDK")
            
            if (!Places.isInitialized()) {
                if (apiKey.isNotBlank() && apiKey != "YOUR_API_KEY_HERE") {
                    try {
                        Places.initialize(requireContext(), apiKey)
                        android.util.Log.d("SearchFragment", "Places SDK initialized successfully")
                    } catch (initException: Exception) {
                        android.util.Log.e("SearchFragment", "Places.initialize() failed: ${initException.message}", initException)
                        placesClient = null
                    }
                } else {
                    android.util.Log.w("SearchFragment", "Google Maps API key is empty or placeholder")
                    placesClient = null
                }
            }
            
            // Verify Places is initialized before creating client
            if (Places.isInitialized() && placesClient == null) {
                try {
                    placesClient = Places.createClient(requireContext())
                    android.util.Log.d("SearchFragment", "Places client created successfully")
                } catch (clientException: Exception) {
                    android.util.Log.e("SearchFragment", "Failed to create Places client: ${clientException.message}", clientException)
                    placesClient = null
                }
            } else if (!Places.isInitialized()) {
                android.util.Log.e("SearchFragment", "Places not initialized - cannot create client")
                placesClient = null
            }
        } catch (e: Exception) {
            android.util.Log.e("SearchFragment", "Unexpected error in Places SDK setup: ${e.message}", e)
            placesClient = null
        }
        
        // Setup RecyclerView
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerRouteNodes)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        
        // Only create adapter if Places client is available
        if (placesClient != null) {
            try {
                adapter = RouteNodeAdapter(
                    mutableListOf(RouteNodeAdapter.RouteNodeData(no = 1)),
                    placesClient!!
                )
                recycler.adapter = adapter
            } catch (e: Exception) {
                android.util.Log.e("SearchFragment", "Error creating RouteNodeAdapter", e)
                Toast.makeText(requireContext(), "Error setting up route editor", Toast.LENGTH_SHORT).show()
            }
        } else {
            android.util.Log.w("SearchFragment", "Places client is null - route editor will not be available")
            // Show a message but don't crash - user can still use other features
            val errorView = android.widget.TextView(requireContext()).apply {
                text = "Location services unavailable.\nPlease configure Google Maps API key in local.properties"
                gravity = android.view.Gravity.CENTER
                setPadding(32, 32, 32, 32)
            }
            recycler.visibility = View.GONE
            // Could add errorView to parent layout if needed
        }
        
        // Setup Ask AI button
        val buttonAskAI = view.findViewById<MaterialButton>(R.id.buttonAskAI)
        buttonAskAI.setOnClickListener {
            askAIForAdvice()
        }
        
        // Setup Save Route button
        val buttonSaveRoute = view.findViewById<MaterialButton>(R.id.buttonSaveRoute)
        buttonSaveRoute?.setOnClickListener {
            saveRoute()
        }
        
        // Observe ViewModel
        observeViewModel()
        
        // Observe RouteViewModel for save route feedback
        try {
            observeRouteViewModel()
        } catch (e: Exception) {
            android.util.Log.e("SearchFragment", "Error setting up RouteViewModel observer", e)
        }
    }
    
    private fun askAIForAdvice() {
        // Show loading state
        viewModel.askAIForAdvice()
    }
    
    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val button = view?.findViewById<MaterialButton>(R.id.buttonAskAI)
            button?.isEnabled = !isLoading
            button?.text = if (isLoading) "Loading..." else "Ask AI for advice !"
        }
        
        // Observe AI response
        viewModel.aiResponse.observe(viewLifecycleOwner) { response ->
            // Show response in a dialog
            showAIResponseDialog(response)
        }
        
        // Observe errors
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showAIResponseDialog(response: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("AI Advice")
            .setMessage(response)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun saveRoute() {
        // Check if adapter is initialized
        if (!::adapter.isInitialized) {
            Toast.makeText(requireContext(), "Route adapter not ready", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check if user is logged in
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Toast.makeText(requireContext(), "Please log in to save routes", Toast.LENGTH_LONG).show()
            android.util.Log.w("SearchFragment", "User not logged in - cannot save route")
            return
        }
        
        // Validate route has at least 2 waypoints with locations
        val validNodes = adapter.getItems().filter { it.location.isNotBlank() }
        if (validNodes.size < 2) {
            Toast.makeText(requireContext(), "Please add at least 2 locations to save a route", Toast.LENGTH_SHORT).show()
            return
        }
        
        android.util.Log.d("SearchFragment", "Preparing to save route with ${validNodes.size} waypoints")
        
        // Show dialog to get route name and visibility
        val routeNameInput = EditText(requireContext())
        routeNameInput.hint = "Route Name"
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Save Route")
            .setView(routeNameInput)
            .setPositiveButton("Save as Public") { _, _ ->
                val routeName = routeNameInput.text.toString().takeIf { it.isNotBlank() } 
                    ?: "Route ${System.currentTimeMillis()}"
                val route = createRouteFromNodes(routeName, validNodes, isPublic = true)
                android.util.Log.d("SearchFragment", "Saving public route: $routeName")
                android.util.Log.d("SearchFragment", "Route data: title=${route.title}, distance=${route.distanceKm}, waypoints=${route.waypoints.size}")
                routeViewModel.saveRoute(route, isPublic = true)
            }
            .setNeutralButton("Save as Private") { _, _ ->
                val routeName = routeNameInput.text.toString().takeIf { it.isNotBlank() } 
                    ?: "Route ${System.currentTimeMillis()}"
                val route = createRouteFromNodes(routeName, validNodes, isPublic = false)
                android.util.Log.d("SearchFragment", "Saving private route: $routeName")
                android.util.Log.d("SearchFragment", "Route data: title=${route.title}, distance=${route.distanceKm}, waypoints=${route.waypoints.size}")
                routeViewModel.saveRoute(route, isPublic = false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createRouteFromNodes(routeName: String, nodes: List<RouteNodeAdapter.RouteNodeData>, isPublic: Boolean): Route {
        // Convert nodes to waypoints (using placeholder GeoPoints - in real app, geocode addresses)
        val waypoints = nodes.map { 
            // For now, use placeholder. In production, geocode the location string
            GeoPoint(0.0, 0.0) 
        }
        
        // Calculate total distance (sum of individual distances)
        val totalDistance = nodes.sumOf { 
            it.distance.toDoubleOrNull() ?: 0.0 
        }
        
        // Create description from nodes
        val description = nodes.joinToString(" → ") { it.location.takeIf { loc -> loc.isNotBlank() } ?: "Location ${it.no}" }
        
        return Route(
            title = routeName,
            description = description,
            waypoints = waypoints,
            distanceKm = totalDistance,
            isPublic = isPublic,
            tags = emptyList(),
            estimatedDurationMinutes = (totalDistance * 12).toInt(), // Rough estimate: 12 min/km walking
            difficulty = "easy"
        )
    }
    
    private fun observeRouteViewModel() {
        routeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val button = view?.findViewById<MaterialButton>(R.id.buttonSaveRoute)
            button?.isEnabled = !isLoading
            button?.text = if (isLoading) "Saving..." else "Save Route"
        }
        
        routeViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        
        routeViewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                android.util.Log.d("SearchFragment", "Route saved successfully to Firebase!")
                Toast.makeText(requireContext(), "Route saved successfully! Check Firebase console to verify.", Toast.LENGTH_LONG).show()
                routeViewModel.saveSuccess.value = false // Reset
            }
        }
    }
}