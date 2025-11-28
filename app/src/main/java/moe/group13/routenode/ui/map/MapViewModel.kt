package moe.group13.routenode.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.data.repository.RouteRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class MapViewModel(private val repository: RouteRepository) : ViewModel() {

    // Favorites
    private val _favorites = MutableLiveData<List<Route>>()
    val favorites: LiveData<List<Route>> = _favorites

    // Polyline points from Directions API
    private val _polylinePoints = MutableLiveData<List<LatLng>>()
    val polylinePoints: LiveData<List<LatLng>> = _polylinePoints

    // Optional: errors
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favList = repository.getFavorites()
                _favorites.postValue(favList)
            } catch (e: Exception) {
                _favorites.postValue(emptyList())
                _errorMessage.postValue(e.message)
            }
        }
    }

  //gets the overview polyline from directions api
    fun fetchPolyline(origin: LatLng, route: Route, apiKey: String, mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = buildDirectionsUrl(origin, route, apiKey,mode)
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("HTTP error ${response.code}")
                val body = response.body?.string() ?: throw IOException("Empty response")
                val json = JSONObject(body)
                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) throw IOException("No routes found")
                val overviewPolyline = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")
                // Decode polyline points
                val points = PolyUtil.decode(overviewPolyline)
                _polylinePoints.postValue(points)

            } catch (e: Exception) {
                _errorMessage.postValue(e.message)
            }
        }
    }
    //build the request
    private fun buildDirectionsUrl(origin: LatLng, route: Route, apiKey: String, mode: String): String {
        val originStr = "${origin.latitude},${origin.longitude}"
        var destinationStr = originStr
        val waypointsList = mutableListOf<String>()

        route.waypoints.forEachIndexed { index, point ->
            val coord = "${point.latitude},${point.longitude}"
            if (index == route.waypoints.size - 1) {
                destinationStr = coord
            } else {
                waypointsList.add(coord)
            }
        }

        val waypointsStr = if (waypointsList.isNotEmpty()) waypointsList.joinToString("|") else ""

        val url = StringBuilder("https://maps.googleapis.com/maps/api/directions/json?")
        url.append("origin=$originStr")
        url.append("&destination=$destinationStr")
        if (waypointsStr.isNotEmpty()) url.append("&waypoints=$waypointsStr")
        url.append("&mode=$mode")
        url.append("&key=$apiKey")

        return url.toString()
    }
}
