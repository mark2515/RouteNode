package moe.group13.routenode.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class Route (
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    var waypoints: List<GeoPoint> = emptyList(),
    var distanceKm: Double = 0.0,
    var creatorId: String = "",   // user who created it
    var isPublic: Boolean = false,
    var createdAt: Long = System.currentTimeMillis()
)