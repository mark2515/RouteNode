package moe.group13.routenode.data

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.DocumentId

data class Route (
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val waypoints: List<GeoPoint> = emptyList(),
    val distanceKm: Double = 0.0,
    val creatorId: String = "",   // user who created it
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)