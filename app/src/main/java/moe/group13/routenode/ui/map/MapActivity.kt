package moe.group13.routenode.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
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
    private var googleMap: GoogleMap? = null
    private lateinit var mapViewModel: MapViewModel
    lateinit var favoritesRecycler: RecyclerView
    private var selectedMode: String = "driving"
    private var currentPolyline: Polyline? = null
    private var isMapReady = false


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private val FALLBACK_LOCATION = LatLng(49.2827, -123.1207)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        favoritesRecycler = findViewById(R.id.favorites_recycler)
        favoritesRecycler.layoutManager = LinearLayoutManager(this)


        // MapViewModel setup
        val repository = RouteRepository()
        mapViewModel = ViewModelProvider(this, MapViewModelFactory(repository))
            .get(MapViewModel::class.java)

        // Load favorites only on first creation
        if (savedInstanceState == null) {
            mapViewModel.loadFavorites()
        }
        
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

        mapViewModel.selectedRoute.observe(this) { route ->
            if (route != null && isMapReady) {
                drawMarkersForRoute(route)
            }
        }

        // Observe polyline points
        mapViewModel.polylinePoints.observe(this) { points ->
            if (points.isNotEmpty() && isMapReady && googleMap != null) {
                currentPolyline?.remove()
                currentPolyline = googleMap?.addPolyline(
                    PolylineOptions().addAll(points)
                        .color(Color.BLUE)
                        .width(8f)
                )
            }
        }
        //idea: https://developer.android.com/guide/fragments/communicate
        supportFragmentManager.setFragmentResultListener("editFinished", this) { _, _ ->
            // Reload updated favorites from Firestore
            mapViewModel.loadFavorites()

            // Hide overlay
            findViewById<FrameLayout>(R.id.overlay_fragment_container).visibility = View.GONE
            favoritesRecycler.visibility = View.VISIBLE
        }

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        // Restore selected route after rotation
        mapViewModel.selectedRoute.value?.let { route ->
            drawMarkersForRoute(route)
        }
    }

    private fun onRowClick(route: Route) {
        if (!isMapReady || googleMap == null) return
        
        // Store the selected route in ViewModel (survives rotation)
        mapViewModel.setSelectedRoute(route)
        
        // Draw markers
        drawMarkersForRoute(route)
        
        //get our location and generate polyline for preview
        getCurrentLocation { userLocation ->
            mapViewModel.fetchPolyline(
                userLocation,
                route,
                BuildConfig.GOOGLE_MAPS_API_KEY,
                selectedMode
            )
        }
    }

    private fun drawMarkersForRoute(route: Route) {
        val map = googleMap ?: return
        
        //draw map pins when user clicks
        map.clear()

        //draw the markers
        route.waypoints.forEachIndexed { index, point ->
            val latLng = LatLng(point.latitude, point.longitude)
            // use the tag at the same index if it exists, else fallback to route.title
            val markerTitle = route.tags.getOrNull(index) ?: route.title
            map.addMarker(
                MarkerOptions().position(latLng)
                    .title(markerTitle)
            )
        }

        //zoom to the first area
        route.waypoints.firstOrNull()?.let { first ->
            val latLng = LatLng(first.latitude, first.longitude)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
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
            route.tags.forEach { tag ->
                baseUrl.append("$tag/")
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
            .addToBackStack("edit_fragment")
            .commit()
        findViewById<FrameLayout>(R.id.overlay_fragment_container).visibility = View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save fragment visibility state
        outState.putInt("overlay_visibility", findViewById<FrameLayout>(R.id.overlay_fragment_container).visibility)
        outState.putInt("recycler_visibility", favoritesRecycler.visibility)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore fragment visibility state
        val overlayVisibility = savedInstanceState.getInt("overlay_visibility", View.GONE)
        val recyclerVisibility = savedInstanceState.getInt("recycler_visibility", View.VISIBLE)
        
        findViewById<FrameLayout>(R.id.overlay_fragment_container).visibility = overlayVisibility
        favoritesRecycler.visibility = recyclerVisibility
    }


    //get current location so we can start doing directions and polylines
    private fun getCurrentLocation(onLocationReady: (LatLng) -> Unit) {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
