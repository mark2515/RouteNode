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
import com.google.android.material.card.MaterialCardView
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
    
    // Edit mode state
    private var isEditMode = false
    private var editingRouteId: String? = null
    private var editingRouteTitle: String? = null
    private var isWaitingForAiToSave = false // Flag to track if we're waiting for AI response before saving
    private var originalRouteNodeDataJson: String? = null // Store original route data to detect changes
    
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
        
        // Setup edit mode buttons
        setupEditModeButtons(view)
        
        // Check if we need to load route data for editing
        checkForEditRoute()
    }
    
    private fun setupEditModeButtons(view: View) {
        val editBanner = view.findViewById<MaterialCardView>(R.id.editModeBanner)
        val editModeText = view.findViewById<android.widget.TextView>(R.id.editModeText)
        val buttonCancel = view.findViewById<MaterialButton>(R.id.buttonCancelEdit)
        val buttonDone = view.findViewById<MaterialButton>(R.id.buttonDoneEdit)
        
        buttonCancel.setOnClickListener {
            cancelEditMode()
        }
        
        buttonDone.setOnClickListener {
            // Show confirmation dialog before saving
            showConfirmSaveDialog()
        }
        
        // Make the name clickable to edit
        editModeText.setOnClickListener {
            showRenameFavoriteDialog()
        }
    }
    
    private fun checkForEditRoute() {
        val prefs = requireContext().getSharedPreferences("route_edit", Context.MODE_PRIVATE)
        val routeNodeDataJson = prefs.getString("edit_route_node_data", null)
        val routeId = prefs.getString("edit_route_id", null)
        val routeTitle = prefs.getString("edit_route_title", null)
        val routeDescription = prefs.getString("edit_route_description", null)
        
        if (routeNodeDataJson != null && routeNodeDataJson.isNotEmpty() && routeId != null) {
            try {
                val gson = Gson()
                val routeNodeData = gson.fromJson(routeNodeDataJson, Array<RouteNodeAdapter.RouteNodeData>::class.java).toList()
                if (routeNodeData.isNotEmpty() && ::routeNodeAdapter.isInitialized) {
                    // Store original JSON BEFORE setting data to adapter
                    originalRouteNodeDataJson = routeNodeDataJson
                    // Enter edit mode
                    enterEditMode(routeId, routeTitle ?: "Unknown Route", routeDescription)
                    routeNodeAdapter.setRouteNodeData(routeNodeData)
                    // Clear the edit flag from preferences
                    prefs.edit().apply {
                        remove("edit_route_node_data")
                        remove("edit_route_id")
                        remove("edit_route_title")
                        remove("edit_route_description")
                        remove("edit_route_distance")
                        apply()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchFragment", "Error loading route data", e)
            }
        }
    }
    
    private fun enterEditMode(routeId: String, routeTitle: String, originalDescription: String?) {
        isEditMode = true
        editingRouteId = routeId
        editingRouteTitle = routeTitle
        
        // Reset the waiting flag
        isWaitingForAiToSave = false
        
        // Note: originalRouteNodeDataJson should already be set in checkForEditRoute()
        // before calling this function
        
        // Show edit mode banner
        view?.findViewById<MaterialCardView>(R.id.editModeBanner)?.visibility = View.VISIBLE
        updateEditModeText(routeTitle)
        
        // Reset Done button to default state
        view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.isEnabled = true
        view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.text = "Done"
        
        // Keep Ask AI button visible in edit mode so users can regenerate AI response
        view?.findViewById<MaterialButton>(R.id.buttonAskAI)?.visibility = View.VISIBLE
        
        // Load original AI response if available (from route description)
        // The description contains the AI response (first 200 chars) if it exists
        if (!originalDescription.isNullOrBlank()) {
            // Check if description looks like an AI response (not just node data)
            val isAiResponse = !originalDescription.contains("Node") || originalDescription.length > 100
            if (isAiResponse) {
                viewModel.aiResponse.value = originalDescription
                // Set AI response in adapter
                routeNodeAdapter.setAiResponse(originalDescription)
            }
        }
    }
    
    private fun updateEditModeText(title: String) {
        view?.findViewById<android.widget.TextView>(R.id.editModeText)?.text = "Editing: $title"
    }
    
    private fun showRenameFavoriteDialog() {
        val currentName = editingRouteTitle ?: "Unknown Route"
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(currentName)
        input.selectAll()
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Rename Favorite")
            .setMessage("Enter a new name for this favorite:")
            .setView(input)
            .setPositiveButton("Save", null) // Set to null first, then set listener after creation
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        // Set positive button listener after dialog creation to control dismissal
        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val newName = input.text.toString().trim()
                if (newName.isBlank()) {
                    Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Update the editing route title
                editingRouteTitle = newName
                // Update the display text
                updateEditModeText(newName)
                // Dismiss dialog
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun exitEditMode() {
        isEditMode = false
        editingRouteId = null
        editingRouteTitle = null
        isWaitingForAiToSave = false
        originalRouteNodeDataJson = null
        
        // Hide edit mode banner
        view?.findViewById<MaterialCardView>(R.id.editModeBanner)?.visibility = View.GONE
        
        // Show Ask AI button
        view?.findViewById<MaterialButton>(R.id.buttonAskAI)?.visibility = View.VISIBLE
        
        // Reset the form
        routeNodeAdapter.reset()
        viewModel.clearAiResponse()
    }
    
    private fun cancelEditMode() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Editing")
            .setMessage("Are you sure you want to cancel? All changes will be lost.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                exitEditMode()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun showConfirmSaveDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Changes")
            .setMessage("Are you sure with the changes?")
            .setPositiveButton("OK") { _, _ ->
                saveEditedFavorite()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun saveEditedFavorite() {
        val routeNodeData = routeNodeAdapter.getRouteNodeData()
        
        if (routeNodeData.isEmpty()) {
            Toast.makeText(requireContext(), "No route data to save", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (editingRouteId == null) {
            Toast.makeText(requireContext(), "Error: Route ID not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate that all fields are filled
        if (!routeNodeAdapter.isAllFieldsValid()) {
            routeNodeAdapter.showAllValidationErrors()
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check if route data has changed by comparing JSON
        val gson = Gson()
        val currentRouteNodeDataJson = gson.toJson(routeNodeData)
        val hasRouteChanged = currentRouteNodeDataJson != originalRouteNodeDataJson
        
        android.util.Log.d("SearchFragment", "Original JSON: ${originalRouteNodeDataJson?.take(100)}")
        android.util.Log.d("SearchFragment", "Current JSON: ${currentRouteNodeDataJson.take(100)}")
        android.util.Log.d("SearchFragment", "Has route changed: $hasRouteChanged")
        
        if (!hasRouteChanged && originalRouteNodeDataJson != null) {
            // No changes to route data, save directly without generating new AI
            android.util.Log.d("SearchFragment", "No changes detected, saving without AI generation")
            performSaveAfterAi()
        } else {
            // Route data has changed, generate new AI advice
            // Set flag to wait for AI response before saving
            isWaitingForAiToSave = true
            
            // Disable Done button and show loading state
            view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.isEnabled = false
            view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.text = "Generating AI advice..."
            
            // Automatically generate AI advice with current route data
            askAIForAdvice()
        }
    }
    
    private fun performSaveAfterAi() {
        val routeNodeData = routeNodeAdapter.getRouteNodeData()
        val aiResponse = viewModel.aiResponse.value
        
        if (routeNodeData.isEmpty()) {
            Toast.makeText(requireContext(), "No route data to save", Toast.LENGTH_SHORT).show()
            isWaitingForAiToSave = false
            view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.isEnabled = true
            view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.text = "Done"
            return
        }
        
        if (editingRouteId == null) {
            Toast.makeText(requireContext(), "Error: Route ID not found", Toast.LENGTH_SHORT).show()
            isWaitingForAiToSave = false
            view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.isEnabled = true
            view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.text = "Done"
            return
        }
        
        // Use the original name (or current editingRouteTitle if it was changed via rename dialog)
        val favoriteName = editingRouteTitle ?: "Updated Route"
        saveEditedFavoriteWithName(routeNodeData, aiResponse, favoriteName)
    }
    
    private fun saveEditedFavoriteWithName(
        routeNodeData: List<RouteNodeAdapter.RouteNodeData>,
        aiResponse: String?,
        favoriteName: String
    ) {
        if (editingRouteId == null) {
            Toast.makeText(requireContext(), "Error: Route ID not found", Toast.LENGTH_SHORT).show()
            isWaitingForAiToSave = false
            view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.isEnabled = true
            view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.text = "Done"
            return
        }
        
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
        
        // Get current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        val creatorId = currentUser?.uid ?: ""
        
        if (creatorId.isEmpty()) {
            Toast.makeText(requireContext(), "Please log in to save favorites", Toast.LENGTH_SHORT).show()
            isWaitingForAiToSave = false
            view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.isEnabled = true
            view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.text = "Done"
            return
        }
        
        // Convert route node data to JSON
        val gson = Gson()
        val routeNodeDataJson = gson.toJson(routeNodeData)
        
        // Create updated Route object with same ID and new name
        val updatedRoute = Route(
            id = editingRouteId!!,
            title = favoriteName,
            description = routeDescription,
            waypoints = emptyList(),
            distanceKm = totalDistance,
            creatorId = creatorId,
            isPublic = false,
            tags = emptyList(),
            estimatedDurationMinutes = (totalDistance * 12).toInt(),
            difficulty = "easy",
            rating = 0.0,
            ratingCount = 0,
            favoriteCount = 0,
            imageUrl = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            routeNodeDataJson = routeNodeDataJson
        )
        
        // Update the favorite by saving with the same ID (will overwrite existing)
        routeViewModel.saveFavorite(updatedRoute, favoriteName)
        
        Toast.makeText(requireContext(), "Favorite updated successfully!", Toast.LENGTH_SHORT).show()
        
        // Reset flag
        isWaitingForAiToSave = false
        
        // Exit edit mode
        exitEditMode()
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
            
            // If loading finished and we're waiting to save, check if we got a response
            if (!isLoading && isWaitingForAiToSave) {
                val aiResponse = viewModel.aiResponse.value
                if (aiResponse != null && aiResponse.isNotBlank()) {
                    performSaveAfterAi()
                } else {
                    // If no response after loading, save anyway with node data
                    performSaveAfterAi()
                }
            }
        }
        
        // Observe AI response
        viewModel.aiResponse.observe(viewLifecycleOwner) { response ->
            routeNodeAdapter.setAiResponse(response)
            // Scroll to bottom when AI response is received
            view?.findViewById<RecyclerView>(R.id.recyclerRouteNodes)?.post {
                val recycler = view?.findViewById<RecyclerView>(R.id.recyclerRouteNodes)
                recycler?.smoothScrollToPosition(routeNodeAdapter.itemCount - 1)
            }
            
            // If we were waiting for AI response to save, perform the save now
            if (isWaitingForAiToSave && response != null && response.isNotBlank()) {
                performSaveAfterAi()
            }
        }
        
        // Observe errors
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                // If we were waiting for AI and got an error, still save with current data
                if (isWaitingForAiToSave) {
                    performSaveAfterAi()
                }
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
        // Don't allow saving as new favorite while in edit mode
        if (isEditMode) {
            Toast.makeText(requireContext(), "Please use 'Done' button to save changes", Toast.LENGTH_SHORT).show()
            return
        }
        
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