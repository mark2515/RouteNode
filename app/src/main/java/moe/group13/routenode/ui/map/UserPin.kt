package moe.group13.routenode.ui.map

import com.google.android.gms.maps.model.LatLng

//class used to format pin
//@TODO: Maybe be relevant to junh
data class UserPin (
    //first destination name
    val destination: String,
    //username?
    val userName: String,
    //location
    val location : LatLng
)