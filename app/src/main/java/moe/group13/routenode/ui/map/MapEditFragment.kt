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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            if (selectedTag == null || selectedPlaceName == null) {
                Toast.makeText(requireContext(), "Please select a tag and place", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedLatLng = selectedPlace!!.latLng
            if (selectedLatLng == null) {
                Toast.makeText(requireContext(), "Invalid location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Disable button to prevent multiple clicks
            swapButton.isEnabled = false
            
            //swap using coroutine
            lifecycleScope.launch {
                val success = withContext(Dispatchers.IO) {
                    routeRepository.swap(routeId!!, selectedTag, selectedPlaceName, selectedLatLng)
                }
                
                if (success) {
                    //tell activity that fragment swap is done
                    parentFragmentManager.setFragmentResult("editFinished", Bundle())
                    //hide fragment
                    activity?.findViewById<FrameLayout>(R.id.overlay_fragment_container)?.visibility =
                        View.GONE
                    //bring back the favorite list to be visible again
                    (activity as? MapActivity)?.favoritesRecycler?.visibility = View.VISIBLE
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Failed to update route", Toast.LENGTH_SHORT).show()
                    swapButton.isEnabled = true
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