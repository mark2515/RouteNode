package moe.group13.routenode.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import moe.group13.routenode.R
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.Polyline


class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapViewModel: MapViewModel

    // for later, to take a list from the favorites fragment
    private var destinations: List<LatLng> = emptyList()

    private var userLatLng = LatLng(49.2827, -123.1207) //if it nulls out, set it to Vancouver

    //@TODO: User Settings prefeences should change this
    private var mode = "walking"

    //store userPins to save having to call fetch for toggle visibility
    // markers are an object
    // idea: https://developers.google.com/android/reference/com/google/android/gms/maps/model/Marker
    private var userPins: MutableList<Marker> = mutableListOf()
    private var isCameraCentered = false


    private lateinit var togglePin: Button

    //fusedLocation stuff, for continuous tracking
    //documentation: https://developer.android.com/develop/sensors-and-location/location/request-updates
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    // Users marker to track
    private var userMarker: Marker? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserRouteAdapter

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        //setup Spinner
        setupModeSpinner()
        //back arrow
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //init fusedLocation
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Source https://stackoverflow.com/questions/66489605/is-constructor-locationrequest-deprecated-in-google-maps-v2
        // callback every 5 seconds
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(1f)
            .build()
        // runs on main thread, because we need to update theUI
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                userLatLng = LatLng(location.latitude, location.longitude)
                //draw the pin for the users current position
                if (::googleMap.isInitialized) {
                    if (userMarker == null) {
                        userMarker = googleMap.addMarker(
                            MarkerOptions()
                                .position(userLatLng)
                                .title("You are here")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    } else {
                        userMarker?.position = userLatLng
                    }
                    // center the camera ONLY ONCE
                    if (!isCameraCentered) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                        isCameraCentered = true
                    }
                }
            }
        }
        //init ViewModel
        mapViewModel = ViewModelProvider(this)[MapViewModel::class.java]
        observeViewModel()
        //TODO: Implement pulling data from intent, not being done yet
        val latitudes: DoubleArray = intent.getDoubleArrayExtra("latitudes") ?: doubleArrayOf()
        val longitudes: DoubleArray = intent.getDoubleArrayExtra("longitudes") ?: doubleArrayOf()
        if (latitudes.size == longitudes.size) {
            destinations = latitudes.indices.map { i ->
                LatLng(latitudes[i], longitudes[i])
            }
        } else {
            Toast.makeText(this, "Latitude/Longitude arrays mismatch", Toast.LENGTH_SHORT).show()
        }
        //TODO: Hardcoded testing fragments
        // Flow: Users current position -> SFU -> Stanley Park
        destinations = listOf(
            LatLng(49.2781, -122.9199), // SFU: waypoint
            LatLng(49.3043, -123.1443)  // Stanley Park: destination
        )
        // Map Frag
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    //runs on the main thread, needs to show the polylines on the ui

    private fun observeViewModel() {
        recyclerView = findViewById(R.id.user_routes_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val pins = mapViewModel.fetchUserStarts()
        adapter = UserRouteAdapter(pins) { pin ->
            openGoogleMaps(pin)
        }
        adapter = UserRouteAdapter(pins) { pin ->
            //resusing code from Markers
            val message = StringBuilder()
            message.append("Do you want to start this route? \n\n")
            pin.destination.indices.forEach { i ->
                message.append("Stop ${i + 1}: ${pin.destination[i]}\n")
                message.append("Location: ${pin.location[i].latitude}, ${pin.location[i].longitude}\n\n")
            }
            AlertDialog.Builder(this)
                .setTitle("${pin.userName} Route")
                .setMessage(message.toString())
                .setPositiveButton("Yes") { dialog, _ ->
                    openGoogleMaps(pin)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        recyclerView.adapter = adapter
    }

    //populate the spinner in activity_map.xml}
    //TODO: Since moving to Google Maps, this will be used to filter the routes recyclerview
    //runs on main thread, but then switches to IO due to fetchDirections
    //
    private fun setupModeSpinner() {
        val spinner = findViewById<Spinner>(R.id.mode_spinner)
        val modes = listOf("Walking", "Driving", "Public Transit")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            modes
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        // set a listener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                mode = when (modes[position]) {
                    "Walking" -> "walking"
                    "Driving" -> "driving"
                    "Public Transit" -> "transit"
                    //TODO: Account settings set it here
                    else -> "walking"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // Enable basic UI
        googleMap.uiSettings.isZoomControlsEnabled = true
        // Check for location permission
        requestLocationPermission()
        // add user pins
        //colour change idea: https://www.youtube.com/watch?v=g-YnGyBdV-s
        addUserPin()
    }

    //main thread
    // Closes MapActivity and goes back to MainActivity with backarrow
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // request location permissions
    //main thread
    private fun requestLocationPermission() {
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
        } else {
            startLocationUpdates()
        }
    }

    //main thread
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //main thread
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    //add using pins function
    //currently main, will swtich to IO, as TODO: print from firebase
    @SuppressLint("PotentialBehaviorOverride")
    fun addUserPin() {
        val pins = mapViewModel.fetchUserStarts()

        //remove existing pins
        userPins.forEach { it.remove() }
        userPins.clear()
        //creation of Marker Object
        pins.forEach { pin ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    //grab the starting point
                    .position(pin.location[0])
                    .title("${pin.userName} Route")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                    .snippet("Click for route details")

            )
            //assign the marker the user's UserPin
            marker?.tag = pin
            userPins.add(marker!!)

            // display other users route and ask if they want to join it
        }
        googleMap.setOnMarkerClickListener { marker ->
            //message builder
            val pin = marker.tag as? UserPin
            if (pin != null) {
                val message = StringBuilder()
                message.append("Do you want to start this route? \n\n")
                pin.destination.indices.forEach { i ->
                    message.append("Stop ${i + 1}: ${pin.destination[i]}\n")
                    message.append("Location: ${pin.location[i].latitude}, ${pin.location[i].longitude}\n\n")
                }
                AlertDialog.Builder(this)
                    .setTitle(marker.title)
                    .setMessage(message.toString())
                    .setPositiveButton("Yes") { dialog, _ ->
                        openGoogleMaps(pin)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true

            }
            true
        }
    }

    //documentation: https://developer.android.com/guide/components/google-maps-intents
    fun openGoogleMaps(pin: UserPin) {
        //if the pin is empty, return
        if (pin.location.isEmpty()) return

        // user position
        val origin = userLatLng
        //use first location, pin origin
        val destination = pin.location.last()


        //beging building the waypoints string
        var waypoints = ""
        if (pin.location.size > 1) {
            //everything but the last
            for (i in 0 until pin.location.size - 1) {
                val loc = pin.location[i]
                waypoints += "${loc.latitude},${loc.longitude}"
            }

        }
        //build the request
        val uri = "https://www.google.com/maps/dir/?api=1" +
                "&origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&waypoints=$waypoints" +
                "&travelmode=walking"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")

        startActivity(intent)

    }
}
