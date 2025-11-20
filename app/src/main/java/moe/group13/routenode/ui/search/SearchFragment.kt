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
import com.google.firebase.firestore.GeoPoint
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.ui.RouteNodeAdapter
import moe.group13.routenode.ui.routes.RouteViewModel

class SearchFragment : Fragment() {
    
    private lateinit var viewModel: SearchViewModel
    private val routeViewModel: RouteViewModel by viewModels()
    private lateinit var placesClient: PlacesClient
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
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(requireContext())
        
        // Setup RecyclerView
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerRouteNodes)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = RouteNodeAdapter(
            mutableListOf(RouteNodeAdapter.RouteNodeData(no = 1)),
            placesClient
        )
        recycler.adapter = adapter
        
        // Setup Ask AI button
        val buttonAskAI = view.findViewById<MaterialButton>(R.id.buttonAskAI)
        buttonAskAI.setOnClickListener {
            askAIForAdvice()
        }
        
        // Setup Save Route button
        val buttonSaveRoute = view.findViewById<MaterialButton>(R.id.buttonSaveRoute)
        buttonSaveRoute.setOnClickListener {
            saveRoute()
        }
        
        // Observe ViewModel
        observeViewModel()
        
        // Observe RouteViewModel for save route feedback
        observeRouteViewModel()
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
        // Validate route has at least 2 waypoints with locations
        val validNodes = adapter.items.filter { it.location.isNotBlank() }
        if (validNodes.size < 2) {
            Toast.makeText(requireContext(), "Please add at least 2 locations to save a route", Toast.LENGTH_SHORT).show()
            return
        }
        
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
                routeViewModel.saveRoute(route, isPublic = true)
            }
            .setNeutralButton("Save as Private") { _, _ ->
                val routeName = routeNameInput.text.toString().takeIf { it.isNotBlank() } 
                    ?: "Route ${System.currentTimeMillis()}"
                val route = createRouteFromNodes(routeName, validNodes, isPublic = false)
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
                Toast.makeText(requireContext(), "Route saved successfully!", Toast.LENGTH_SHORT).show()
                routeViewModel.saveSuccess.value = false // Reset
            }
        }
    }
}