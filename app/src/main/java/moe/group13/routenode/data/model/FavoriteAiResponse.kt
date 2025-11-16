package moe.group13.routenode.data.model

import com.google.firebase.firestore.DocumentId

data class FavoriteAiResponse (
    @DocumentId val id: String = "",
    val userId: String,
    val startLocation: String,
    val targetLocation: String,
    val aiResponse: String
)



