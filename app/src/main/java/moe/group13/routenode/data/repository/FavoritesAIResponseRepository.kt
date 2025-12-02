package moe.group13.routenode.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import moe.group13.routenode.data.model.FavoriteAiResponse
import moe.group13.routenode.data.model.Route


class FavoritesAIResponseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
)
{
    private fun uid(): String{
        return auth.currentUser?.uid ?: throw Exception("User not logged in")
    }

    private val userAiFavoriteCollection = db.collection("userAiResponse")
    suspend fun saveFavorite(uid: String, favoriteAiResponse: FavoriteAiResponse){
        val currentTime = System.currentTimeMillis()
        val favoriteWithTimestamp = favoriteAiResponse.copy(savedAt = currentTime)
        userAiFavoriteCollection
            .document(uid)
            .collection("favoriteAiResponse")
            .document(favoriteWithTimestamp.id)
            .set(favoriteWithTimestamp)
            .await()
    }
    suspend fun getFavorites(uid: String): List<FavoriteAiResponse>{
        return userAiFavoriteCollection
            .document(uid)
            .collection("favoriteAiResponse")
            .orderBy("savedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(FavoriteAiResponse::class.java)
    }
    suspend fun deleteFavorite(uid: String, favoriteId: String){
        userAiFavoriteCollection
            .document(uid)
            .collection("favoriteAiResponse")
            .document(favoriteId)
            .delete()
            .await()
    }




}
