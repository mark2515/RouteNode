package moe.group13.routenode.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class Route(
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    var waypoints: List<GeoPoint> = emptyList(),
    var distanceKm: Double = 0.0,
    var creatorId: String = "",   // user who created it
    var creatorName: String = "", // optional: creator's display name
    var isPublic: Boolean = false,
    var tags: List<String> = emptyList(), // e.g., ["scenic", "coffee", "parks"]
    var estimatedDurationMinutes: Int = 0, // estimated travel time
    var difficulty: String = "easy", // easy, medium, hard
    var rating: Double = 0.0, // average rating (0-5)
    var ratingCount: Int = 0, // number of ratings
    var favoriteCount: Int = 0, // number of users who favorited
    var imageUrl: String = "", // optional route image
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var routeNodeDataJson: String = "" // JSON string storing route node data for editing
)