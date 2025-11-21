package moe.group13.routenode.ui.map


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
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

    fun fetchDirections(
        origin: LatLng?, destination: LatLng, waypoints: List<LatLng>,
        apiKey: String, mode: String
    ) {
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
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                errorMessage.postValue(e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) {
                    errorMessage.postValue("No routes found")
                    return
                }
                // Polyline
                //overview_polyline is Googles way of encoding the entire route
                val overviewPolyline =
                    routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points")
                //decode the encoded polyline, which turns it into a list of lat and long
                val path: List<LatLng> = PolyUtil.decode(overviewPolyline)
                polylinePoints.postValue(path)

                // Step-by-step instructions
                // JSON Structure:routes -> legs -> steps.html_instructions
                // By default, Google Directions give use 1 route
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                val instructions = StringBuilder()

                for (legIndex in 0 until legs.length()) {
                    val leg = legs.getJSONObject(legIndex)
                    val startAddress = leg.getString("start_address")
                    val endAddress = leg.getString("end_address")
                    instructions.append("Route from $startAddress to $endAddress:\n\n")
                    val steps = leg.getJSONArray("steps")
                    //waypoints: https://developers.google.com/maps/documentation/directions/get-directions#Waypoints
                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        val travelMode = step.getString("travel_mode")
                        //waypoints does not support public transit, so only works for one
                        // TODO: Maybe call this for every destination given?
                        if (travelMode == "TRANSIT") {
                            val transit = step.getJSONObject("transit_details")
                            val departure =
                                transit.getJSONObject("departure_stop").getString("name")
                            val arrival = transit.getJSONObject("arrival_stop").getString("name")
                            val lineObj = transit.getJSONObject("line")
                            val line =
                                if (lineObj.has("short_name")) lineObj.getString("short_name")
                                else lineObj.getString("name")
                            val headsign = transit.getString("headsign")
                            instructions.append("Take $line toward $headsign from $departure to $arrival\n\n")
                        } else {
                            val htmlInstruction = step.getString("html_instructions")
                            instructions.append(android.text.Html.fromHtml(htmlInstruction))
                            instructions.append("\n\n")
                        }
                    }
                }
                instructionsText.postValue(instructions.toString())
            }
        })

    }

    //TESTING
    fun testPins(): List<UserPin> {
        return listOf(
            UserPin("Stanley Park", "edengler01", LatLng(49.3043, -123.1443)),
            UserPin("Downtown Vancouver", "user02", LatLng(49.2827, -123.1207)),
            UserPin("Granville Island", "user03", LatLng(49.2715, -123.1341)),
            UserPin("SFU Burnaby", "user04", LatLng(49.2781, -122.9199)),
            UserPin("Queen Elizabeth Park", "user05", LatLng(49.2415, -123.1120))
        )


    }

}