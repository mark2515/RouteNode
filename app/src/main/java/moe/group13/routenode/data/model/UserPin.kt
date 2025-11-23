package moe.group13.routenode.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class UserPin(
    @DocumentId var id: String = "",
    // List of destination names
    var destination: List<String> = emptyList(),
    // Username of the user who set the pin
    var userName: String = "",
    // Location coordinates stored as GeoPoint (Firestore compatible)
    var location: List<GeoPoint> = emptyList()
) {
    
     // Convert GeoPoint list to LatLng list for Google Maps usage
    
    fun toLatLngList(): List<LatLng> {
        return location.map { geoPoint ->
            LatLng(geoPoint.latitude, geoPoint.longitude)
        }
    }
    

    // Create UserPin from LatLng list (converts to GeoPoint for storage)
    companion object {
        fun fromLatLngList(
            destination: List<String>,
            userName: String,
            locations: List<LatLng>
        ): UserPin {
            val geoPoints = locations.map { latLng ->
                GeoPoint(latLng.latitude, latLng.longitude)
            }
            return UserPin(
                destination = destination,
                userName = userName,
                location = geoPoints
            )
        }
    }
}

