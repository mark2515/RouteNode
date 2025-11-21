package moe.group13.routenode.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.button.MaterialButton
import moe.group13.routenode.R
import moe.group13.routenode.ui.RouteNodeAdapter

class SearchFragment : Fragment() {
    
    private lateinit var viewModel: SearchViewModel
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
        
        routeNodeAdapter = RouteNodeAdapter(
            mutableListOf(RouteNodeAdapter.RouteNodeData(no = 1)),
            placesClient
        ) {
            askAIForAdvice()
        }
        recycler.adapter = routeNodeAdapter
        
        // Setup Ask AI button
        val buttonAskAI = view.findViewById<MaterialButton>(R.id.buttonAskAI)
        buttonAskAI.setOnClickListener {
            askAIForAdvice()
        }
        
        // Observe ViewModel
        observeViewModel()
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
            routeNodeAdapter.setAiResponse(response)
        }
        
        // Observe errors
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }
}