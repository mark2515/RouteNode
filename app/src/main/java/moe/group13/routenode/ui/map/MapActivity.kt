package moe.group13.routenode.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import moe.group13.routenode.BuildConfig
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.data.repository.RouteRepository


class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var googleMap: GoogleMap
    private lateinit var mapViewModel: MapViewModel
    lateinit var favoritesRecycler: RecyclerView
    private lateinit var modeSpinner: Spinner
    private var selectedMode: String = "driving"
    private var currentPolyline: Polyline? = null


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private val FALLBACK_LOCATION = LatLng(49.2827, -123.1207)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        favoritesRecycler = findViewById(R.id.favorites_recycler)
        favoritesRecycler.layoutManager = LinearLayoutManager(this)

        //spinner setup
        modeSpinner = findViewById(R.id.mode_spinner)
        val modes = listOf("Driving", "Walking", "Bicycling")
        val modeValues = listOf("driving", "walking", "bicycling")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modeSpinner.adapter = adapter

        modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedMode = modeValues[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case where nothing is selected
            }
        }
        // MapViewModel setup
        val repository = RouteRepository()
        mapViewModel = ViewModelProvider(this, MapViewModelFactory(repository))
            .get(MapViewModel::class.java)

        mapViewModel.loadFavorites()
        mapViewModel.favorites.observe(this) { favList ->
            favoritesRecycler.adapter = FavoritesAdapter(
                favorites = favList,
                onGoClick = ::openGoogleMaps,
                onPreviewClick = ::onRowClick,
                onEditClick = ::onEditClick
            )
        }

        // Initialize map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun onRowClick(route: Route) {
        if (!::googleMap.isInitialized) return
        //draw map pins when user clicks
        googleMap.clear()

        //draw the markers
        route.waypoints.forEachIndexed { index, point ->
            val latLng = LatLng(point.latitude, point.longitude)
            // use the tag at the same index if it exists, else fallback to route.title
            val markerTitle = route.tags.getOrNull(index) ?: route.title
            googleMap.addMarker(
                MarkerOptions().position(latLng)
                    .title(markerTitle)
            )
        }

        //zoom to the first area
        route.waypoints.firstOrNull()?.let { first ->
            val latLng = LatLng(first.latitude, first.longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
        //get our location and generate polyline for preview
        getCurrentLocation { userLocation ->
            mapViewModel.fetchPolyline(
                userLocation,
                route,
                BuildConfig.GOOGLE_MAPS_API_KEY,
                selectedMode
            )
        }
        //draw the polyline
        mapViewModel.polylinePoints.observe(this) { points ->
            if (points.isNotEmpty()) {
                currentPolyline?.remove()
                currentPolyline = googleMap.addPolyline(
                    PolylineOptions().addAll(points)
                        .addAll(points)
                        .color(Color.BLUE)
                        .width(8f)
                )
            }
        }
    }

    //idea: https://developer.android.com/guide/components/google-maps-intents
    //built upon: https://developer.android.com/training/cars/platforms/automotive-os/android-intents-automotive
    private fun openGoogleMaps(route: Route) {
        if (route.waypoints.isEmpty()) return
        // Build the directions URL with waypoints and users location
        getCurrentLocation { userLocation ->
            val baseUrl = StringBuilder("https://www.google.com/maps/dir/")
            baseUrl.append("${userLocation.latitude},${userLocation.longitude}/")
            route.waypoints.forEach { waypoint ->
                baseUrl.append("${waypoint.latitude},${waypoint.longitude}/")
            }
            val gmmIntentUri = Uri.parse(baseUrl.toString())
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

            //open google maps: https://developer.android.com/guide/components/intents-common#ViewMap
            mapIntent.setPackage("com.google.android.apps.maps")
            //check if maps is installed: //check if googlemaps installed: idea: https://stackoverflow.com/questions/65564947/how-to-create-maps-intent-in-such-a-way-that-if-maps-app-is-not-installed-on-use
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                Toast.makeText(
                    this,
                    "Google Maps is not installed. Opening in browser...",
                    Toast.LENGTH_LONG
                ).show()
                //fallback: idea: https://stackoverflow.com/questions/5248870/starting-an-action-view-activity-to-open-the-browser-how-do-i-return-to-my-app
                val browserIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                startActivity(browserIntent)
            }
            startActivity(mapIntent)
        }

    }


    //open edit fragment
    //passing data idea: https://stackoverflow.com/questions/42266436/passing-objects-between-fragments
    private fun onEditClick(route: Route) {
        favoritesRecycler.visibility = View.GONE
        val editFragment = MapEditFragment.newInstance(route.id)

        supportFragmentManager.beginTransaction()
            .replace(R.id.overlay_fragment_container, editFragment)
            .addToBackStack(null)
            .commit()
        findViewById<FrameLayout>(R.id.overlay_fragment_container).visibility = View.VISIBLE
    }


    //get current location so we can start doing directions and polylines
    private fun getCurrentLocation(onLocationReady: (LatLng) -> Unit) {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationReady(LatLng(location.latitude, location.longitude))
            } else {
                onLocationReady(FALLBACK_LOCATION)
            }
        }
    }
}
