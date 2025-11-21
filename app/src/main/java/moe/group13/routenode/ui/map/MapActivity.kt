package moe.group13.routenode.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import moe.group13.routenode.BuildConfig
import moe.group13.routenode.R
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapViewModel: MapViewModel

    // for later, to take a list from the favorites fragment
    private var destinations: List<LatLng> = emptyList()

    //testing purposes
    //private var destinationLatLng: LatLng? = null
    private var userLatLng: LatLng? = null

    //default mode for directions
    private var mode = "walking"

    //store userPins to save having to call fetch for toggle visibility
    // markers are an object
    // idea: https://developers.google.com/android/reference/com/google/android/gms/maps/model/Marker
    private var userPins: MutableList<Marker> = mutableListOf()

    //hide the pins initially
    private var isPinsVisible = false
    private lateinit var togglePin: Button

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
        destinations = listOf(
            LatLng(49.2781, -122.9199), // SFU
            LatLng(49.3043, -123.1443)  // Stanley Park
        )
        // Map Frag
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // button press to toggle visibility
        togglePin = findViewById<Button>(R.id.toggle_pins_button)
        togglePin.setOnClickListener {
            //if pins is visible, turn it to invisible
            if (!isPinsVisible) {
                //if userPins is empty: first startup
                if (userPins.isEmpty()) {
                    userPins.forEach { it.isVisible = true }
                }
                addUserPin()
                isPinsVisible = true
                togglePin.text = "Hide Pins"
            }
            //if pins is invisible, turn it visible
            else {
                userPins.forEach { it.isVisible = false }
                isPinsVisible = false
                togglePin.text = "Show Pins"

            }
        }


    }

    private fun observeViewModel() {
        mapViewModel.polylinePoints.observe(this) { path ->
            if (::googleMap.isInitialized) {
                googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(path)
                        .width(10f)
                        .color(android.graphics.Color.GREEN)
                )
                googleMap.addMarker(
                    MarkerOptions()
                        .position(path.last())
                        .title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
            }
        }
        mapViewModel.instructionsText.observe(this) { text ->
            findViewById<TextView>(R.id.directions_text).text = text
        }
        mapViewModel.errorMessage.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    //populate the spinner in activity_map.xml}
    //TODO: Possibly set user's preferred mode of transport as default
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
                    //TODO: Set it here
                    else -> "walking"
                }
                // change the directions on change
                if (userLatLng != null && destinations.isNotEmpty()) {
                    //transit only supports one destination
                    if (mode == "transit")
                        mapViewModel.fetchDirections(
                            userLatLng!!,
                            destinations[0],
                            emptyList(),
                            BuildConfig.GOOGLE_MAPS_API_KEY,
                            mode
                        ) else {
                        val origin = userLatLng
                        val destination = destinations.last()
                        val waypoints = destinations.dropLast(1)
                        mapViewModel.fetchDirections(
                            origin,
                            destination,
                            waypoints,
                            BuildConfig.GOOGLE_MAPS_API_KEY,
                            mode
                        )
                    }
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

    }

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

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getUserLocation()
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        googleMap.isMyLocationEnabled = true

        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                userLatLng = currentLatLng
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))

                //prep if everything is valid
                if (userLatLng != null && destinations.isNotEmpty()) {
                    // Directions API: If we have route to A -> B -> C -> D
                    // A is origin, B,C, are waypoints (inbetween) and D is destination
                    val origin = userLatLng
                    val destination = destinations.last()
                    val waypoints = destinations.dropLast(1)
                    mapViewModel.fetchDirections(
                        origin,
                        destination,
                        waypoints,
                        BuildConfig.GOOGLE_MAPS_API_KEY, mode
                    )
                }

            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //add using pins function
    fun addUserPin() {
        val pins = mapViewModel.fetchUserStarts()
        //creation of Marker Object
        pins.forEach { pin ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(pin.location)
                    .title("${pin.destination} (${pin.userName})")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
            )
            userPins.add(marker!!)
        }

    }

}
