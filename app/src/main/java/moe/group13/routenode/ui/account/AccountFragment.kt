package moe.group13.routenode.ui.account

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import moe.group13.routenode.R
import moe.group13.routenode.auth.SignInActivity
import org.json.JSONObject
import moe.group13.routenode.BuildConfig
import android.location.Geocoder

class AccountFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var profileImage: ImageView

    private lateinit var weatherCity: TextView
    private lateinit var weatherEmoji: TextView
    private lateinit var weatherTemp: TextView
    private lateinit var weatherStatus: TextView

    private val requestCodeLocation = 2001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myProfileBtn = view.findViewById<LinearLayout>(R.id.myProfileBtn)
        val settingsBtn = view.findViewById<LinearLayout>(R.id.SettingsBtn)
        val aiModelsBtn = view.findViewById<LinearLayout>(R.id.AIModelsBtn)
        val logoutBtn = view.findViewById<LinearLayout>(R.id.LogoutBtn)

        nameTextView = view.findViewById(R.id.NameTV)
        emailTextView = view.findViewById(R.id.EmailTV)
        profileImage = view.findViewById(R.id.profileImageView)

        // WEATHER UI
        weatherCity = view.findViewById(R.id.txtWeatherCity)
        weatherEmoji = view.findViewById(R.id.txtWeatherEmoji)
        weatherTemp = view.findViewById(R.id.txtWeatherTemp)
        weatherStatus = view.findViewById(R.id.txtWeatherStatus)

        loadAccountInfo()

        myProfileBtn.setOnClickListener {
            startActivity(Intent(requireContext(), MyProfileActivity::class.java))
        }

        settingsBtn.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        aiModelsBtn.setOnClickListener {
            startActivity(Intent(requireContext(), AIModelsActivity::class.java))
        }

        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        checkLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        loadAccountInfo()
    }

    private fun loadAccountInfo() {
        val user = auth.currentUser ?: return
        emailTextView.text = user.email ?: "Unknown Email"

        db.collection("users")
            .document(user.uid)
            .collection("profile")
            .document("info")
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "User"
                val photoUrl = doc.getString("photoUrl")

                nameTextView.text = name

                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(photoUrl)
                        .into(profileImage)
                }
            }
    }

    private fun checkLocationPermission() {
        val ctx = requireContext()

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestCodeLocation
            )
        } else {
            loadUserLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == requestCodeLocation &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            loadUserLocation()
        }
    }

    private fun loadUserLocation() {
        val context = requireContext()

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            weatherStatus.text = "No permission"
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(context)

        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    getCityName(loc.latitude, loc.longitude) { city ->
                        loadWeather(loc.latitude, loc.longitude, city)
                    }
                } else {
                    weatherStatus.text = "Location unavailable"
                }
            }
        } catch (e: SecurityException) {
            weatherStatus.text = "Permission error"
        }
    }

    private fun getCityName(lat: Double, lon: Double, callback: (String) -> Unit) {
        try {
            val geocoder = Geocoder(requireContext())
            val results = geocoder.getFromLocation(lat, lon, 1)

            if (!results.isNullOrEmpty()) {
                val city = results[0].locality
                    ?: results[0].subAdminArea
                    ?: results[0].adminArea
                    ?: "Unknown Location"

                callback(city)
            } else {
                callback("Unknown Location")
            }
        } catch (e: Exception) {
            callback("Unknown Location")
        }
    }

    private fun loadWeather(lat: Double, lon: Double, city: String) {
        val apiKey = BuildConfig.OPENWEATHER_API_KEY

        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"

        val queue = Volley.newRequestQueue(requireContext())

        val request = StringRequest(
            com.android.volley.Request.Method.GET,
            url,
            { response ->
                val json = JSONObject(response)
                val temp = json.getJSONObject("main").getDouble("temp").toInt()
                val condition = json.getJSONArray("weather").getJSONObject(0).getString("main")

                val emoji = when (condition.lowercase()) {
                    "rain" -> "🌧️"
                    "clouds" -> "☁️"
                    "clear" -> "☀️"
                    "snow" -> "❄️"
                    "thunderstorm" -> "⛈️"
                    "mist", "fog", "haze" -> "🌫️"
                    else -> "🌤️"
                }

                weatherCity.text = city
                weatherEmoji.text = emoji
                weatherTemp.text = "$temp°C"
                weatherStatus.text = condition
            },
            {
                weatherCity.text = city
                weatherStatus.text = "Error"
            }
        )

        queue.add(request)
    }
}
