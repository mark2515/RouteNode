package moe.group13.routenode.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class UserPreferences(
    @DocumentId var id: String = "",
    var userId: String = "",
    var preferredPlaces: List<PreferredPlace> = emptyList(),
    var unitPreference: String = "km", // km, miles
    var theme: String = "light", // light, dark, system
    var notificationsEnabled: Boolean = true,
    var routeUpdatesEnabled: Boolean = true,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

data class PreferredPlace(
    var name: String = "",
    var address: String = "",
    var location: GeoPoint? = null,
    var placeType: String = "", // home, work, favorite, custom
    var notes: String = ""
)

