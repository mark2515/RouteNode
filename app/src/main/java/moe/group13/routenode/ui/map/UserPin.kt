package moe.group13.routenode.ui.map

import com.google.android.gms.maps.model.LatLng

//class used to format pin
//@TODO: Maybe be relevant to junh
data class UserPin (
    //@TODO: User ID here?
    //first destination name
    val destination: List<String>,
    //username?
    val userName: String,
    //location
    val location : List<LatLng>
)