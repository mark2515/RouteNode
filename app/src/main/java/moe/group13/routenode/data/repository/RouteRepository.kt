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
    private val favoritesCollection = db.collection("favorites")

    private fun uid(): String {
        return auth.currentUser?.uid ?: throw Exception("User not logged in")
    }

    // Public Routes
    suspend fun getPublicRoutes(): List<Route> {
        return try {
            publicRoutesCollection
                .whereEqualTo("isPublic", true)
                .get()
                .await()
                .toObjects(Route::class.java)
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error getting public routes", e)
            emptyList()
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
            .addOnFailureListener { e ->
                Log.e("ROUTE_REPO", "Error getting public routes", e)
                callback(emptyList())
            }
    }

    // Save route to public collection
    fun saveRoute(route: Route, callback: (Boolean, String?) -> Unit) {
        val currentUid = try {
            uid()
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "User not logged in", e)
            callback(false, null)
            return
        }

        val routeWithCreator = route.copy(creatorId = currentUid)
        Log.d("ROUTE_REPO", "Attempting to save route to Firebase:")
        Log.d("ROUTE_REPO", "  Collection: routes_public")
        Log.d("ROUTE_REPO", "  Title: ${routeWithCreator.title}")
        Log.d("ROUTE_REPO", "  Creator ID: $currentUid")
        Log.d("ROUTE_REPO", "  Distance: ${routeWithCreator.distanceKm} km")
        Log.d("ROUTE_REPO", "  Is Public: ${routeWithCreator.isPublic}")
        
        publicRoutesCollection
            .add(routeWithCreator)
            .addOnSuccessListener { doc ->
                Log.d("ROUTE_REPO", "SUCCESS: Route saved to Firebase!")
                Log.d("ROUTE_REPO", "  Document ID: ${doc.id}")
                Log.d("ROUTE_REPO", "  Collection path: routes_public/${doc.id}")
                Log.d("ROUTE_REPO", "  Check Firebase Console > Firestore > routes_public collection")
                callback(true, doc.id)
            }
            .addOnFailureListener { e ->
                Log.e("ROUTE_REPO", "ERROR: Failed to save route to Firebase", e)
                Log.e("ROUTE_REPO", "Error details: ${e.message}")
                callback(false, null)
            }
    }

    // Save route to user's personal collection
    suspend fun saveUserRoute(route: Route): String? {
        return try {
            val currentUid = uid()
            val routeWithCreator = route.copy(creatorId = currentUid)
            Log.d("ROUTE_REPO", "Attempting to save private route to Firebase:")
            Log.d("ROUTE_REPO", "  Collection: user/$currentUid/routes")
            Log.d("ROUTE_REPO", "  Title: ${routeWithCreator.title}")
            Log.d("ROUTE_REPO", "  Distance: ${routeWithCreator.distanceKm} km")
            
            val docRef = userRoutesCollection
                .document(currentUid)
                .collection("routes")
                .add(routeWithCreator)
                .await()
            Log.d("ROUTE_REPO", "SUCCESS: Private route saved to Firebase!")
            Log.d("ROUTE_REPO", "  Document ID: ${docRef.id}")
            Log.d("ROUTE_REPO", "  Collection path: user/$currentUid/routes/${docRef.id}")
            Log.d("ROUTE_REPO", "  Check Firebase Console > Firestore > user collection")
            docRef.id
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "ERROR: Failed to save private route to Firebase", e)
            Log.e("ROUTE_REPO", "Error details: ${e.message}")
            null
        }
    }

    suspend fun getUserRoutes(uid: String? = null): List<Route> {
        return try {
            val targetUid = uid ?: uid()
            userRoutesCollection
                .document(targetUid)
                .collection("routes")
                .get()
                .await()
                .toObjects(Route::class.java)
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error getting user routes", e)
            emptyList()
        }
    }

    // Update route
    suspend fun updateRoute(routeId: String, route: Route): Boolean {
        return try {
            val currentUid = uid()
            // Check if user owns the route
            val existingRoute = publicRoutesCollection
                .document(routeId)
                .get()
                .await()
                .toObject(Route::class.java)

            if (existingRoute?.creatorId == currentUid) {
                publicRoutesCollection
                    .document(routeId)
                    .set(route)
                    .await()
                Log.d("ROUTE_REPO", "Updated route: $routeId")
                true
            } else {
                Log.e("ROUTE_REPO", "User does not own this route")
                false
            }
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error updating route", e)
            false
        }
    }

    // Delete route
    suspend fun deleteRoute(routeId: String): Boolean {
        return try {
            val currentUid = uid()
            val existingRoute = publicRoutesCollection
                .document(routeId)
                .get()
                .await()
                .toObject(Route::class.java)

            if (existingRoute?.creatorId == currentUid) {
                publicRoutesCollection
                    .document(routeId)
                    .delete()
                    .await()
                Log.d("ROUTE_REPO", "Deleted route: $routeId")
                true
            } else {
                Log.e("ROUTE_REPO", "User does not own this route")
                false
            }
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error deleting route", e)
            false
        }
    }

    // Favorites
    suspend fun saveFavorite(route: Route): Boolean {
        return try {
            val currentUid = uid()
            favoritesCollection
                .document(currentUid)
                .collection("routes")
                .document(route.id)
                .set(route)
                .await()
            Log.d("ROUTE_REPO", "Saved favorite: ${route.id}")
            true
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error saving favorite", e)
            false
        }
    }

    suspend fun removeFavorite(routeId: String): Boolean {
        return try {
            val currentUid = uid()
            favoritesCollection
                .document(currentUid)
                .collection("routes")
                .document(routeId)
                .delete()
                .await()
            Log.d("ROUTE_REPO", "Removed favorite: $routeId")
            true
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error removing favorite", e)
            false
        }
    }

    suspend fun isFavorite(routeId: String): Boolean {
        return try {
            val currentUid = uid()
            val doc = favoritesCollection
                .document(currentUid)
                .collection("routes")
                .document(routeId)
                .get()
                .await()
            doc.exists()
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error checking favorite", e)
            false
        }
    }

    suspend fun getFavorites(uid: String? = null): List<Route> {
        return try {
            val targetUid = uid ?: uid()
            favoritesCollection
                .document(targetUid)
                .collection("routes")
                .get()
                .await()
                .toObjects(Route::class.java)
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error getting favorites", e)
            emptyList()
        }
    }
}