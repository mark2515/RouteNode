package moe.group13.routenode.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import moe.group13.routenode.data.model.Route

class RouteRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val publicRoutesCollection = db.collection("routes_public")
    private val userRoutesCollection = db.collection("user")
    private val favroitesCollection = db.collection("favorites")


    private fun uid(): String{
        return auth.currentUser?.uid ?: throw Exception("User not logged in")
    }



    suspend fun getPublicRoutes(): List<Route> {
        return publicRoutesCollection.get().await().toObjects(Route::class.java)
    }

    suspend fun getUserRoutes(uid: String): List<Route> {
        return userRoutesCollection
            .document(uid)
            .collection("routes")
            .get()
            .await()
            .toObjects(Route::class.java)
    }

//    suspend fun saveRoute(uid: String, route: Route){
//        userRoutesCollection.document(uid)
//            .collection("routes")
//            .add(route)
//            .await()
//    }
    fun saveRoute(route: Route, callback: (Boolean, String?) -> Unit) {
        publicRoutesCollection
            .add(route)
            .addOnSuccessListener { doc ->
                Log.d("ROUTE_REPO", "Saved route: ${doc.id}")
                callback(true, doc.id)
            }
            .addOnFailureListener { e ->
                Log.e("ROUTE_REPO", "Error saving route", e)
                callback(false, null)
            }
    }

    fun getPublicRoutes(callback: (List<Route>) -> Unit) {
        publicRoutesCollection
            .whereEqualTo("isPublic", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val routes = snapshot.toObjects(Route::class.java)
                callback(routes)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    suspend fun saveFavorite(uid: String, route: Route){
        db.collection("favorites")
            .document(uid)
            .collection("routes")
            .document(route.id)
            .set(route)
            .await()
    }

    suspend fun getFavorites(uid: String): List<Route> {
        return favroitesCollection.document(uid)
            .collection("routes")
            .get()
            .await()
            .toObjects(Route::class.java)
    }
}