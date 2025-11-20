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
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import android.view.MenuItem
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.PolyUtil




class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        //back arrow
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Map Frag
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Enable basic UI
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Check for location permission
        requestLocationPermission()

        // grab routeLatitude
        val destLat = intent.getDoubleExtra("dest_lat", Double.NaN)
        val destLng = intent.getDoubleExtra("dest_lng", Double.NaN)
        if (!destLat.isNaN() && !destLng.isNaN()) {
            val destination = LatLng(destLat, destLng)
            googleMap.addMarker(MarkerOptions().position(destination).title("Destination"))
        }



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

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
                //TESTING:
                fetchDirections(
                    originLat = location.latitude,
                    originLng = location.longitude,
                    destLat = 49.3043,
                    destLng = -123.1443,
                    apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
                )

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

    //make a call to google directions api, needs lat and lng for the origin and destination
    fun fetchDirections(originLat: Double, originLng: Double, destLat: Double, destLng: Double, apiKey: String) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$originLat,$originLng&destination=$destLat,$destLng&key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object: Callback{
            override fun onFailure(call: Call, e: IOException){
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response){
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) return


                //poly lines
                val overviewPolyline = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")

                val path: List<LatLng> = PolyUtil.decode(overviewPolyline)
                runOnUiThread {
                    //Draw
                    val polyLineOptions = PolylineOptions()
                        .addAll(path)
                        .color(android.graphics.Color.GREEN)
                        .width(10f)
                    googleMap.addPolyline(polyLineOptions)
                }
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                val steps = legs.getJSONObject(0).getJSONArray("steps")
                //text instructions
                val instructions = StringBuilder()
                for (i in 0 until steps.length()) {
                    val step = steps.getJSONObject(i)
                    val htmlInstruction = step.getString("html_instructions")
                    instructions.append(android.text.Html.fromHtml(htmlInstruction))
                    instructions.append("\n\n")
                }

                runOnUiThread {
                    val directionsText: TextView = findViewById(R.id.directions_text)
                    directionsText.text = instructions.toString()
                }
            }
        })
    }

}
