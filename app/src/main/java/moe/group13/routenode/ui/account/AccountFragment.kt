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

    // State variables to preserve across rotation
    private var cachedName: String? = null
    private var cachedEmail: String? = null
    private var cachedPhotoUrl: String? = null

    // Weather state to preserve across rotation
    private var cachedWeatherCity: String? = null
    private var cachedWeatherEmoji: String? = null
    private var cachedWeatherTemp: String? = null
    private var cachedWeatherStatus: String? = null

    companion object {
        private const val KEY_NAME = "cached_name"
        private const val KEY_EMAIL = "cached_email"
        private const val KEY_PHOTO_URL = "cached_photo_url"
        private const val KEY_WEATHER_CITY = "cached_weather_city"
        private const val KEY_WEATHER_EMOJI = "cached_weather_emoji"
        private const val KEY_WEATHER_TEMP = "cached_weather_temp"
        private const val KEY_WEATHER_STATUS = "cached_weather_status"
    }

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

        // Restore cached data if available
        if (savedInstanceState != null) {
            cachedName = savedInstanceState.getString(KEY_NAME)
            cachedEmail = savedInstanceState.getString(KEY_EMAIL)
            cachedPhotoUrl = savedInstanceState.getString(KEY_PHOTO_URL)

            // Immediately display cached data to prevent flicker
            if (cachedName != null) {
                nameTextView.text = cachedName
            }
            if (cachedEmail != null) {
                emailTextView.text = cachedEmail
            }
            if (cachedPhotoUrl != null && isAdded) {
                Glide.with(requireContext())
                    .load(cachedPhotoUrl)
                    .into(profileImage)
            }

            // Restore cached weather UI
            cachedWeatherCity = savedInstanceState.getString(KEY_WEATHER_CITY)
            cachedWeatherEmoji = savedInstanceState.getString(KEY_WEATHER_EMOJI)
            cachedWeatherTemp = savedInstanceState.getString(KEY_WEATHER_TEMP)
            cachedWeatherStatus = savedInstanceState.getString(KEY_WEATHER_STATUS)

            if (cachedWeatherCity != null) {
                weatherCity.text = cachedWeatherCity
            }
            if (cachedWeatherEmoji != null) {
                weatherEmoji.text = cachedWeatherEmoji
            }
            if (cachedWeatherTemp != null) {
                weatherTemp.text = cachedWeatherTemp
            }
            if (cachedWeatherStatus != null) {
                weatherStatus.text = cachedWeatherStatus
            }
        }

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

        // Only load location & weather if we don't already have cached weather
        if (cachedWeatherCity == null || cachedWeatherStatus == null) {
            checkLocationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAccountInfo()
    }

    private fun loadAccountInfo() {
        val user = auth.currentUser ?: return
        val email = user.email ?: "Unknown Email"
        emailTextView.text = email
        cachedEmail = email

        db.collection("users")
            .document(user.uid)
            .collection("profile")
            .document("info")
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "User"
                val photoUrl = doc.getString("photoUrl")

                // Cache the data for rotation
                cachedName = name
                cachedPhotoUrl = photoUrl

                nameTextView.text = name

                if (!photoUrl.isNullOrEmpty() && isAdded) {
                    Glide.with(requireContext())
                        .load(photoUrl)
                        .into(profileImage)
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current profile data to survive rotation
        cachedName?.let { outState.putString(KEY_NAME, it) }
        cachedEmail?.let { outState.putString(KEY_EMAIL, it) }
        cachedPhotoUrl?.let { outState.putString(KEY_PHOTO_URL, it) }

        // Save current weather data to survive rotation
        cachedWeatherCity?.let { outState.putString(KEY_WEATHER_CITY, it) }
        cachedWeatherEmoji?.let { outState.putString(KEY_WEATHER_EMOJI, it) }
        cachedWeatherTemp?.let { outState.putString(KEY_WEATHER_TEMP, it) }
        cachedWeatherStatus?.let { outState.putString(KEY_WEATHER_STATUS, it) }
    }

    private fun checkLocationPermission() {
        val ctx = requireContext()

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
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

                // Cache weather data
                cachedWeatherCity = city
                cachedWeatherEmoji = emoji
                cachedWeatherTemp = "$temp°C"
                cachedWeatherStatus = condition

                weatherCity.text = cachedWeatherCity
                weatherEmoji.text = cachedWeatherEmoji
                weatherTemp.text = cachedWeatherTemp
                weatherStatus.text = cachedWeatherStatus
            },
            {
                cachedWeatherCity = city
                cachedWeatherStatus = "Error"

                weatherCity.text = cachedWeatherCity
                weatherStatus.text = cachedWeatherStatus
            }
        )

        queue.add(request)
    }
}
