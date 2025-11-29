package moe.group13.routenode.ui.search

import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.ui.routes.RouteViewModel

class EditModeManager(
    private val fragment: Fragment,
    private val routeViewModel: RouteViewModel,
    private val routeNodeAdapter: RouteNodeAdapter,
    private val viewModel: SearchViewModel,
    private val onTriggerAiGeneration: () -> Unit = {}
) {
    var isEditMode = false
        private set
    var editingRouteId: String? = null
        private set
    var editingRouteTitle: String? = null
        private set
    var isWaitingForAiToSave = false
    var originalRouteNodeDataJson: String? = null
        private set

    fun setupEditModeButtons(view: View) {
        val buttonCancel = view.findViewById<MaterialButton>(R.id.buttonCancelEdit)
        val buttonDone = view.findViewById<MaterialButton>(R.id.buttonDoneEdit)
        val editModeText = view.findViewById<android.widget.TextView>(R.id.editModeText)

        buttonCancel.setOnClickListener {
            showCancelDialog()
        }

        buttonDone.setOnClickListener {
            showConfirmSaveDialog()
        }

        // Make the name clickable to edit
        editModeText.setOnClickListener {
            showRenameFavoriteDialog()
        }
    }

    fun checkForEditRoute() {
        val prefs = fragment.requireContext().getSharedPreferences("route_edit", Context.MODE_PRIVATE)
        val routeNodeDataJson = prefs.getString("edit_route_node_data", null)
        val routeId = prefs.getString("edit_route_id", null)
        val routeTitle = prefs.getString("edit_route_title", null)
        val routeDescription = prefs.getString("edit_route_description", null)

        if (routeNodeDataJson != null && routeNodeDataJson.isNotEmpty() && routeId != null) {
            try {
                val gson = Gson()
                val routeNodeData = gson.fromJson(
                    routeNodeDataJson,
                    Array<RouteNodeAdapter.RouteNodeData>::class.java
                ).toList()
                if (routeNodeData.isNotEmpty()) {
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
                android.util.Log.e("EditModeManager", "Error loading route data", e)
            }
        }
    }

    fun enterEditMode(routeId: String, routeTitle: String, originalDescription: String?) {
        isEditMode = true
        editingRouteId = routeId
        editingRouteTitle = routeTitle

        // Reset the waiting flag
        isWaitingForAiToSave = false

        // Show edit mode banner
        fragment.view?.findViewById<MaterialCardView>(R.id.editModeBanner)?.visibility = View.VISIBLE
        updateEditModeText(routeTitle)

        // Reset Done button to default state
        fragment.view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.isEnabled = true
        fragment.view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.text = "Done"

        // Keep Ask AI button visible in edit mode
        fragment.view?.findViewById<MaterialButton>(R.id.buttonAskAI)?.visibility = View.VISIBLE

        // Load original AI response if available
        if (!originalDescription.isNullOrBlank()) {
            val isAiResponse = !originalDescription.contains("Node") || originalDescription.length > 100
            if (isAiResponse) {
                viewModel.aiResponse.value = originalDescription
                routeNodeAdapter.setAiResponse(originalDescription)
            }
        }
    }

    fun exitEditMode() {
        isEditMode = false
        editingRouteId = null
        editingRouteTitle = null
        isWaitingForAiToSave = false
        originalRouteNodeDataJson = null

        // Hide edit mode banner
        fragment.view?.findViewById<MaterialCardView>(R.id.editModeBanner)?.visibility = View.GONE

        // Show Ask AI button
        fragment.view?.findViewById<MaterialButton>(R.id.buttonAskAI)?.visibility = View.VISIBLE

        // Reset the form
        routeNodeAdapter.reset()
        viewModel.clearAiResponse()
    }

    private fun updateEditModeText(title: String) {
        fragment.view?.findViewById<android.widget.TextView>(R.id.editModeText)?.text = "Editing: $title"
    }

    private fun showRenameFavoriteDialog() {
        val currentName = editingRouteTitle ?: "Unknown Route"
        val input = EditText(fragment.requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(currentName)
        input.selectAll()

        val dialog = AlertDialog.Builder(fragment.requireContext())
            .setTitle("Rename Favorite")
            .setMessage("Enter a new name for this favorite:")
            .setView(input)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val newName = input.text.toString().trim()
                if (newName.isBlank()) {
                    Toast.makeText(fragment.requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                editingRouteTitle = newName
                updateEditModeText(newName)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Cancel Editing")
            .setMessage("Are you sure you want to cancel? All changes will be lost.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                exitEditMode()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showConfirmSaveDialog() {
        AlertDialog.Builder(fragment.requireContext())
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
            Toast.makeText(fragment.requireContext(), "No route data to save", Toast.LENGTH_SHORT).show()
            return
        }

        if (editingRouteId == null) {
            Toast.makeText(fragment.requireContext(), "Error: Route ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate that all fields are filled
        if (!routeNodeAdapter.isAllFieldsValid()) {
            routeNodeAdapter.showAllValidationErrors()
            Toast.makeText(
                fragment.requireContext(),
                "Please fill in all required fields",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Check if route data has changed by comparing JSON
        val gson = Gson()
        val currentRouteNodeDataJson = gson.toJson(routeNodeData)
        val hasRouteChanged = currentRouteNodeDataJson != originalRouteNodeDataJson

        android.util.Log.d("EditModeManager", "Has route changed: $hasRouteChanged")

        if (!hasRouteChanged && originalRouteNodeDataJson != null) {
            // No changes to route data, save directly without generating new AI
            android.util.Log.d("EditModeManager", "No changes detected, saving without AI generation")
            performSaveAfterAi()
        } else {
            // Route data has changed, generate new AI advice
            isWaitingForAiToSave = true

            // Disable Done button and show loading state
            fragment.view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.isEnabled = false
            fragment.view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.text = "Generating AI advice..."

            // Trigger AI advice generation via callback
            onTriggerAiGeneration()
        }
    }

    fun performSaveAfterAi() {
        val routeNodeData = routeNodeAdapter.getRouteNodeData()
        val aiResponse = viewModel.aiResponse.value

        if (routeNodeData.isEmpty()) {
            Toast.makeText(fragment.requireContext(), "No route data to save", Toast.LENGTH_SHORT).show()
            resetSaveState()
            return
        }

        if (editingRouteId == null) {
            Toast.makeText(fragment.requireContext(), "Error: Route ID not found", Toast.LENGTH_SHORT).show()
            resetSaveState()
            return
        }

        val favoriteName = editingRouteTitle ?: "Updated Route"
        saveEditedFavoriteWithName(routeNodeData, aiResponse, favoriteName)
    }

    private fun saveEditedFavoriteWithName(
        routeNodeData: List<RouteNodeAdapter.RouteNodeData>,
        aiResponse: String?,
        favoriteName: String
    ) {
        if (editingRouteId == null) {
            Toast.makeText(fragment.requireContext(), "Error: Route ID not found", Toast.LENGTH_SHORT).show()
            resetSaveState()
            return
        }

        // Calculate total distance
        val totalDistance = routeNodeData.sumOf {
            it.distance.toDoubleOrNull() ?: 0.0
        }

        // Create route description
        val routeDescription = aiResponse ?: run {
            routeNodeData.joinToString("\n") { node ->
                "Node ${node.no}: ${node.location} - ${node.place} (${node.distance} km)"
            }
        }

        // Get current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        val creatorId = currentUser?.uid ?: ""

        if (creatorId.isEmpty()) {
            Toast.makeText(fragment.requireContext(), "Please log in to save favorites", Toast.LENGTH_SHORT).show()
            resetSaveState()
            return
        }

        // Convert route node data to JSON
        val gson = Gson()
        val routeNodeDataJson = gson.toJson(routeNodeData)

        // Create updated Route object
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

        // Update the favorite
        routeViewModel.saveFavorite(updatedRoute, favoriteName)

        Toast.makeText(fragment.requireContext(), "Favorite updated successfully!", Toast.LENGTH_SHORT).show()

        // Reset flag and exit edit mode
        isWaitingForAiToSave = false
        exitEditMode()
    }

    private fun resetSaveState() {
        isWaitingForAiToSave = false
        fragment.view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.isEnabled = true
        fragment.view?.findViewById<MaterialButton>(R.id.buttonDoneEdit)?.text = "Done"
    }

    fun needsAiGeneration(): Boolean {
        return isWaitingForAiToSave
    }
}