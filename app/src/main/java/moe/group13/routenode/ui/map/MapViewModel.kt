package moe.group13.routenode.ui.map


import android.text.Html
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class MapViewModel : ViewModel() {
    val polylinePoints = MutableLiveData<List<LatLng>>()
    val instructionsText = MutableLiveData<String>()
    val errorMessage = MutableLiveData<String?>()


    //fetch user pins for their first destination in the route, so user can look
    //TODO: junhp: Fetch the pins from the database
    fun fetchUserStarts(): List<UserPin> {
        return testPins()
    }

    //runs on IO Thread (Background)
    @Deprecated("Moving to Google Maps")
    suspend fun fetchDirections(
        origin: LatLng?, destination: LatLng, waypoints: List<LatLng>,
        apiKey: String, mode: String
    ) {
        try {
            withContext(Dispatchers.IO) {
                //begin constructing the waypoint parameter for api call
                val waypointsParam = if (waypoints.isNotEmpty()) {
                    "&waypoints=" + waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
                } else {
                    ""
                }

                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${origin?.latitude},${origin?.longitude}" +
                        "&destination=${destination.latitude},${destination.longitude}" +
                        "&mode=$mode" +
                        waypointsParam +
                        "&key=$apiKey"

                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("HTTP error")
                }
                val body = response.body?.string() ?: throw IOException("Empty Response")
                val json = JSONObject(body)
                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) {
                    throw IOException("No routes found")
                }

                // Polyline
                val overviewPolyline =
                    routes.getJSONObject(0).getJSONObject("overview_polyline")
                        .getString("points")
                val path: List<LatLng> = PolyUtil.decode(overviewPolyline)
                polylinePoints.postValue(path)

                // Step-by-step travel instructions
                //route: Google will always return 1 route, will return more if AlternativeRoute flag is set
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                val instructions = StringBuilder()

                //legs: Leg of Journey, the waypoints
                for (legIndex in 0 until legs.length()) {
                    val leg = legs.getJSONObject(legIndex)
                    val startAddress = leg.getString("start_address")
                    val endAddress = leg.getString("end_address")
                    instructions.append("Route from $startAddress to $endAddress:\n\n")
                    val steps = leg.getJSONArray("steps")
                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        val travelMode = step.getString("travel_mode")
                        // Transit mode is a mixture of WALKING and TRANSIT
                        //T ranist has specific fields
                        if (travelMode == "TRANSIT") {
                            val transit = step.getJSONObject("transit_details")
                            val departure =
                                transit.getJSONObject("departure_stop")
                                    .getString("name")
                            val arrival =
                                transit.getJSONObject("arrival_stop").getString("name")
                            val lineObj = transit.getJSONObject("line")
                            val line =
                                if (lineObj.has("short_name")) lineObj.getString("short_name")
                                else lineObj.getString("name")
                            val headsign = transit.getString("headsign")
                            instructions.append("Take $line toward $headsign from $departure to $arrival\n\n")
                        } else {
                            val htmlInstruction = step.getString("html_instructions")
                            instructions.append(Html.fromHtml(htmlInstruction))
                            instructions.append("\n\n")
                        }
                    }
                }
                instructionsText.postValue(instructions.toString())
            }

        } catch (e: Exception) {

        }
    }

    //TESTING
    fun testPins(): List<UserPin> {
        return listOf(
            UserPin(
                destination = listOf("Stanley Park", "Granville Island"),
                userName = "edengler01",
                location = listOf(
                    LatLng(49.3043, -123.1443), // Stanley Park
                    LatLng(49.2715, -123.1341)  // Granville Island
                )
            ),
            UserPin(
                destination = listOf("Downtown Vancouver", "Queen Elizabeth Park"),
                userName = "user02",
                location = listOf(
                    LatLng(49.2827, -123.1207), // Downtown
                    LatLng(49.2415, -123.1120)  // Queen Elizabeth Park
                )
            ),
            UserPin(
                destination = listOf("SFU Burnaby", "Metrotown", "Downtown"),
                userName = "user03",
                location = listOf(
                    LatLng(49.2781, -122.9199), // SFU
                    LatLng(49.2250, -123.0010),  // Metrotown
                    LatLng(49.2827, -123.1207), // Downtown
                )
            ),
            UserPin(
                destination = listOf("Vanier Park"),
                userName = "user04",
                location = listOf(
                    LatLng(49.2696, -123.1400), // Vanier Park
                )
            ),
            UserPin(
                destination = listOf("Science World", "Olympic Village"),
                userName = "user05",
                location = listOf(
                    LatLng(49.2734, -123.1030), // Science World
                    LatLng(49.2710, -123.1260)  // Olympic Village
                )
            )
        )
    }

}
