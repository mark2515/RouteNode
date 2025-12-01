package moe.group13.routenode.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.button.MaterialButton
import moe.group13.routenode.R
import moe.group13.routenode.ui.routes.RouteViewModel

class SearchFragment : Fragment() {

    private lateinit var viewModel: SearchViewModel
    private val routeViewModel: RouteViewModel by viewModels()
    private lateinit var placesClient: PlacesClient
    private lateinit var routeNodeAdapter: RouteNodeAdapter
    
    // Manager classes
    private lateinit var editModeManager: EditModeManager
    private lateinit var favoriteSaveManager: FavoriteSaveManager
    private lateinit var aiAdviceManager: AIAdviceManager

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

        // Initialize adapter first
        val initialData = viewModel.getSavedRouteNodeData()?.toMutableList() 
            ?: mutableListOf(RouteNodeAdapter.RouteNodeData(no = 1))
        
        routeNodeAdapter = RouteNodeAdapter(
            initialData,
            placesClient,
            onRetryAi = {
                if (::aiAdviceManager.isInitialized) {
                    aiAdviceManager.askAIForAdvice()
                }
            },
            onValidationChanged = { isValid ->
            },
            onFavoriteAi = {
                if (::favoriteSaveManager.isInitialized && ::editModeManager.isInitialized) {
                    favoriteSaveManager.saveRouteAsFavorite(editModeManager.isEditMode)
                }
            }
        )
        recycler.adapter = routeNodeAdapter
        
        // Initialize manager classes
        aiAdviceManager = AIAdviceManager(this, viewModel, routeNodeAdapter)
        favoriteSaveManager = FavoriteSaveManager(this, routeViewModel, routeNodeAdapter, viewModel)
        editModeManager = EditModeManager(
            this, 
            routeViewModel, 
            routeNodeAdapter, 
            viewModel,
            onTriggerAiGeneration = {
                aiAdviceManager.askAIForAdvice()
            }
        )

        // Setup Ask AI button
        val buttonAskAI = view.findViewById<MaterialButton>(R.id.buttonAskAI)
        buttonAskAI.setOnClickListener {
            // Check if all fields are valid
            if (!routeNodeAdapter.isAllFieldsValid()) {
                // Show validation errors
                routeNodeAdapter.showAllValidationErrors()
                Toast.makeText(
                    requireContext(),
                    "Please fill in all required fields",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Scroll to the bottom of the RecyclerView
                recycler.post {
                    recycler.smoothScrollToPosition(routeNodeAdapter.itemCount - 1)
                }
                aiAdviceManager.askAIForAdvice()
            }
        }

        // Observe ViewModel
        observeViewModel()

        // Setup edit mode buttons
        editModeManager.setupEditModeButtons(view)

        // Check if we need to load route data for editing
        editModeManager.checkForEditRoute()
    }


    private fun observeViewModel() {

        aiAdviceManager.observeViewModel(
            viewLifecycleOwner,
            onAiResponseReceived = {
                if (editModeManager.isWaitingForAiToSave) {
                    editModeManager.performSaveAfterAi()
                }
            },
            onLoadingFinished = {
                if (editModeManager.isWaitingForAiToSave) {
                    editModeManager.performSaveAfterAi()
                }
            }
        )

        // Observe route view model errors
        routeViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update distance units when returning from settings
        if (::routeNodeAdapter.isInitialized) {
            routeNodeAdapter.updateDistanceUnits()
        }
        // Check for edit route data
        if (::editModeManager.isInitialized) {
            editModeManager.checkForEditRoute()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (::routeNodeAdapter.isInitialized) {
            viewModel.saveRouteNodeData(routeNodeAdapter.getRouteNodeData())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::routeNodeAdapter.isInitialized) {
            routeNodeAdapter.cleanup()
        }
    }
}