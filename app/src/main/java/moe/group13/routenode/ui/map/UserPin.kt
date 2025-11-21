package moe.group13.routenode.ui.map

import com.google.android.gms.maps.model.LatLng

//class used to format pin
data class UserPin (
    //first destination name
    val destination: String,
    //username?
    val userName: String,
    //location
    val location : LatLng
)