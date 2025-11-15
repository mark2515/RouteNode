package moe.group13.routenode.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import moe.group13.routenode.data.Route

class RouteRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private fun uid(): String{
        return auth.currentUser?.uid ?: throw Exception("User not logged in")
    }

    suspend fun saveUserRoute(route: Route): String{
        val uid = uid()
        val ref = db.collection("users").document(uid).collection("routes").document()

        val routeWithId = route.copy(id = ref.id, creatorId = uid, isPublic = false)

        ref.set(routeWithId).await()
        return ref.id
    }

    suspend fun publishRoute(route: Route): String {
        val ref = db.collection("routes_public").document()

        val publicRoute = route.copy(id=ref.id,isPublic=true)
        ref.set(publicRoute).await()
        return ref.id
    }

    suspend fun getPublicRoutes(): List<Route> {
        return db.collection("routes_public").get().await().toObjects(Route::class.java)
    }

    suspend fun getUserRoutes(): List<Route> {
        val uid = uid()

        return db.collection("routes_private")
            .document(uid)
            .collection("routes")
            .get()
            .await()
            .toObjects(Route::class.java)
    }

    suspend fun addFavorite(route: Route){
        val uid = uid()

        db.collection("favorites")
            .document(uid)
            .collection("routes")
            .document(route.id)
            .set(route)
            .await()
    }

    suspend fun removeFavorite(routeId: String) {
        val uid = uid()
        db.collection("favorites")
            .document(uid)
            .collection("routes")
            .document(routeId)
            .delete()
            .await()
    }

    suspend fun getFavorites(): List<Route> {
        val uid = uid()
        return db.collection("favorites")
            .document(uid)
            .collection("routes")
            .get()
            .await()
            .toObjects(Route::class.java)
    }
}
