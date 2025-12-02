package moe.group13.routenode.ui.map

import EditTagAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.gms.common.api.Status
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.data.repository.RouteRepository

class MapEditFragment : Fragment() {

    private var routeId: String? = null
    private var route: Route? = null
    private lateinit var mapViewModel: MapViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var tagAdapter: EditTagAdapter
    private var selectedTags = mutableListOf<String>()
    private lateinit var swapButton: Button
    private var selectedPlace: Place? = null
    private val routeRepository = RouteRepository()

    companion object {
        private const val ARG_ROUTE_ID = "route_id"

        fun newInstance(routeId: String): MapEditFragment {
            val fragment = MapEditFragment()
            val args = Bundle()
            args.putString(ARG_ROUTE_ID, routeId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeId = arguments?.getString(ARG_ROUTE_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_waypoint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swapButton = view.findViewById(R.id.swapButton)
        recyclerView = view.findViewById(R.id.recyclerViewTags)

        // Init Places API if not already initialized
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key))
        }

        // ViewModel setup
        mapViewModel = ViewModelProvider(requireActivity(), MapViewModelFactory(RouteRepository()))
            .get(MapViewModel::class.java)

        // RecyclerView setup
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        tagAdapter = EditTagAdapter(selectedTags)
        recyclerView.adapter = tagAdapter

        // Load route and tags
        routeId?.let { id ->
            mapViewModel.getCurrentUserRouteById(id).observe(viewLifecycleOwner) { r ->
                r?.let {
                    route = it
                    selectedTags.clear()
                    selectedTags.addAll(it.tags)
                    tagAdapter.notifyDataSetChanged()
                }
            }
        }

        // Get the Autocomplete fragment
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        // Configure autocomplete fields
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        )
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                selectedPlace = place
            }

            override fun onError(status: Status) {
            }
        })
        // swap button functionality
        swapButton.setOnClickListener {
            val selectedTag = tagAdapter.getSelectedTag()
            val selectedPlaceName = selectedPlace?.name
            
            // Validate inputs
            if (selectedTag == null) {
                Toast.makeText(requireContext(), "Please select a location to swap", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedPlaceName == null) {
                Toast.makeText(requireContext(), "Please search and select a new address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val selectedLatLng = selectedPlace?.latLng
            if (selectedLatLng == null) {
                Toast.makeText(requireContext(), "Invalid location selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (routeId == null) {
                Toast.makeText(requireContext(), "Route ID is missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            //swap with callback
            routeRepository.swap(routeId!!, selectedTag, selectedPlaceName, selectedLatLng) { success, errorMessage ->
                activity?.runOnUiThread {
                    if (success) {
                        Toast.makeText(requireContext(), "Location updated successfully", Toast.LENGTH_SHORT).show()
                        
                        //tell activity that fragment swap is done
                        parentFragmentManager.setFragmentResult("editFinished", Bundle())
                        //hide fragment
                        activity?.findViewById<FrameLayout>(R.id.overlay_fragment_container)?.visibility =
                            View.GONE
                        //bring back the favorite list to be visible again
                        (activity as? MapActivity)?.favoritesRecycler?.visibility = View.VISIBLE
                        parentFragmentManager.popBackStack()
                    } else {
                        val message = errorMessage ?: "Failed to update location. Please try again."
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        Log.e("MapEditFragment", "Swap failed: $errorMessage")
                    }
                }
            }

        }
    }

    //when the back button is pressed, hide the fragment
    override fun onDestroyView() {
        super.onDestroyView()
        //hide the edit fragment
        activity?.findViewById<FrameLayout>(R.id.overlay_fragment_container)?.visibility = View.GONE

        // Show the favorites list again
        (activity as? MapActivity)?.findViewById<RecyclerView>(R.id.favorites_recycler)?.visibility =
            View.VISIBLE
    }

}