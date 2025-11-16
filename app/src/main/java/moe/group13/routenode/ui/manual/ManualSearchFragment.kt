package moe.group13.routenode.ui.manual

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.coroutines.launch
import moe.group13.routenode.R
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import com.google.android.gms.common.api.Status

// Data class used to extract JSON from places API
data class SimplePlace(
    val id: String,
    val name: String,
    val latLng: LatLng
)


class ManualSearchFragment : Fragment() {

    private lateinit var startContainer: LinearLayout
    private lateinit var destContainer: LinearLayout
    private lateinit var searchButton: Button

    private var startPlace: SimplePlace? = null
    private var destinationPlace: SimplePlace? = null

    private lateinit var db: AppDatabase
    private lateinit var routeDao: RouteDao
    private lateinit var restaurantDao: RestaurantDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_manual_search, container, false)

        // Find views and containers
        startContainer = view.findViewById(R.id.startRestaurantsContainer)
        destContainer = view.findViewById(R.id.destRestaurantsContainer)
        searchButton = view.findViewById(R.id.searchButton)

        // Init DB
        db = AppDatabase.getDatabase(requireContext())
        routeDao = db.routeDao()
        restaurantDao = db.restaurantDao()

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key))
        }

        // Autocomplete fragments
        //idea: https://www.youtube.com/watch?v=QcyaICJ9CNA&list=LL&
        val startFragment = AutocompleteSupportFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.start_fragment_container, startFragment)
            .commitNow()
        startFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        startFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                startPlace = SimplePlace(place.id ?: "", place.name ?: "", place.latLng!!)
            }

            override fun onError(status: Status) {
                Toast.makeText(requireContext(), "Start place error: $status", Toast.LENGTH_SHORT)
                    .show()
            }
        })

        val destFragment = AutocompleteSupportFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.destination_fragment_container, destFragment)
            .commitNow()
        destFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        destFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                destinationPlace = SimplePlace(place.id ?: "", place.name ?: "", place.latLng!!)
            }

            override fun onError(status: Status) {
                Toast.makeText(
                    requireContext(),
                    "Destination place error: $status",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        searchButton.setOnClickListener {
            val startContainer = view.findViewById<LinearLayout>(R.id.startRestaurantsContainer)
            val destContainer = view.findViewById<LinearLayout>(R.id.destRestaurantsContainer)

            if (startPlace == null || destinationPlace == null) {
                Toast.makeText(requireContext(), "Select both places first", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            startContainer.removeAllViews()
            destContainer.removeAllViews()

            fetchNearbyRestaurants(startPlace!!, startContainer)
            fetchNearbyRestaurants(destinationPlace!!, destContainer)
        }

        return view
    }

    //idea: https://www.youtube.com/watch?v=3gqP4qRGkec
    private fun fetchNearbyRestaurants(
        place: SimplePlace,
        container: LinearLayout
    ) {
        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${place.latLng.latitude},${place.latLng.longitude}" +
                "&radius=5000" +
                "&type=restaurant" +
                "&key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CheckpointFragment", "Nearby search failed: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val results = json.getJSONArray("results")

                requireActivity().runOnUiThread {
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val name = item.getString("name")
                        val address = item.optString("vicinity", "")

                        // Create a SimplePlace for this restaurant
                        val restaurantPlace = SimplePlace(
                            id = item.optString("place_id", ""),
                            name = name,
                            latLng = LatLng(
                                item.getJSONObject("geometry").getJSONObject("location")
                                    .getDouble("lat"),
                                item.getJSONObject("geometry").getJSONObject("location")
                                    .getDouble("lng")
                            )
                        )

                        val tv = android.widget.TextView(requireContext())
                        tv.text = "$name\n$address"
                        tv.setPadding(16, 16, 16, 16)
                        tv.textSize = 16f


                        tv.setOnClickListener {
                            lifecycleScope.launch {
                                // Fetch all saved routes from database
                                val savedRoutes: List<RouteEntity> =
                                    routeDao.getAllRoutes() // explicit type
                                val options: MutableList<String> =
                                    savedRoutes.map { it.name }.toMutableList()
                                options.add("Create New Route") // last option
                                val items: Array<String> = options.toTypedArray()
                                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Add $name to a route")
                                    .setItems(items) { dialog: DialogInterface, which: Int ->
                                        lifecycleScope.launch {
                                            if (which == options.size - 1) {
                                                val input =
                                                    android.widget.EditText(requireContext())
                                                androidx.appcompat.app.AlertDialog.Builder(
                                                    requireContext()
                                                )
                                                    .setTitle("New Route Name")
                                                    .setView(input)
                                                    .setPositiveButton("Create") { d: DialogInterface, _: Int ->
                                                        val routeName = input.text.toString()
                                                            .ifEmpty { "Route ${savedRoutes.size + 1}" }
                                                        lifecycleScope.launch {
                                                            val newRouteId: Long =
                                                                routeDao.insertRoute(
                                                                    RouteEntity(name = routeName)
                                                                )
                                                            restaurantDao.insertRestaurant(
                                                                RestaurantEntity(
                                                                    placeId = restaurantPlace.id,
                                                                    name = restaurantPlace.name,
                                                                    lat = restaurantPlace.latLng.latitude,
                                                                    lng = restaurantPlace.latLng.longitude,
                                                                    routeOwnerId = newRouteId
                                                                )
                                                            )
                                                            Toast.makeText(
                                                                requireContext(),
                                                                "${restaurantPlace.name} added to $routeName",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                        d.dismiss()
                                                    }
                                                    .setNegativeButton("Cancel") { d: DialogInterface, _: Int -> d.dismiss() }
                                                    .show()

                                            } else {
                                                val route: RouteEntity =
                                                    savedRoutes[which] // explicit type
                                                restaurantDao.insertRestaurant(
                                                    RestaurantEntity(
                                                        placeId = restaurantPlace.id,
                                                        name = restaurantPlace.name,
                                                        lat = restaurantPlace.latLng.latitude,
                                                        lng = restaurantPlace.latLng.longitude,
                                                        routeOwnerId = route.routeId
                                                    )
                                                )
                                                Toast.makeText(
                                                    requireContext(),
                                                    "${restaurantPlace.name} added to ${route.name}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                // --------------------------------------------------------------------
                                            }
                                            dialog.dismiss()
                                        }
                                    }
                                    .show()
                            }
                        }
                        container.addView(tv)
                    }
                }
            }
        })
    }
}
