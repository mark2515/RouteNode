package moe.group13.routenode.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import moe.group13.routenode.BuildConfig
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.data.repository.RouteRepository


class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var googleMap: GoogleMap
    private lateinit var mapViewModel: MapViewModel
    private lateinit var favoritesRecycler: RecyclerView
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
                onItemClick = ::onRowClick,
                onButtonClick = ::openGoogleMaps
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
        //get our location and generate polyline
        getCurrentLocation { userLocation ->
            mapViewModel.fetchPolyline(userLocation, route, BuildConfig.GOOGLE_MAPS_API_KEY,selectedMode)
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

    private fun openGoogleMaps(route: Route) {
        if (!::googleMap.isInitialized) return
        // Draw pins on the map
        route.waypoints.firstOrNull()?.let { first ->
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude, first.longitude), 15f)
            )
        }
    }

    //for generatine the overviewpolyling
    // Google encodes and ccreates their own route polyline, so we just use it to draw by decoding it
    private fun getDirectionsUrl(userLocation: LatLng, route: Route): String {
        val origin = "${userLocation.latitude},${userLocation.longitude}"
        var destination = origin
        val waypointsList = mutableListOf<String>()

        // Loop through waypoints to separate destination from intermediate points
        route.waypoints.forEachIndexed { index, point ->
            val coord = "${point.latitude},${point.longitude}"
            if (index == route.waypoints.size - 1) {
                destination = coord // last waypoint
            } else {
                waypointsList.add(coord) // intermediate waypoints
            }
        }
        // build the waypoints section
        val waypoints = if (waypointsList.isNotEmpty()) waypointsList.joinToString("|") else ""
        // Begin building the api request
        val url = StringBuilder("https://maps.googleapis.com/maps/api/directions/json?")
        url.append("origin=$origin")
        url.append("&destination=$destination")
        if (waypoints.isNotEmpty()) {
            url.append("&waypoints=$waypoints")
        }
        url.append("&key=${BuildConfig.GOOGLE_MAPS_API_KEY}")

        return url.toString()
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
