package moe.group13.routenode.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
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
        publicRoutesCollection
            .add(routeWithCreator)
            .addOnSuccessListener { doc ->
                Log.d("ROUTE_REPO", "Saved route: ${doc.id}")
                callback(true, doc.id)
            }
            .addOnFailureListener { e ->
                Log.e("ROUTE_REPO", "Error saving route", e)
                callback(false, null)
            }
    }

    // Save route to user's personal collection
    suspend fun saveUserRoute(route: Route): String? {
        return try {
            val currentUid = uid()
            val routeWithCreator = route.copy(creatorId = currentUid)
            val docRef = userRoutesCollection
                .document(currentUid)
                .collection("routes")
                .add(routeWithCreator)
                .await()
            Log.d("ROUTE_REPO", "Saved user route: ${docRef.id}")
            docRef.id
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error saving user route", e)
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
            val currentTime = System.currentTimeMillis()
            // Ensure creatorId is set to the current user who is saving the favorite
            val routeWithCreator = route.copy(
                creatorId = currentUid,
                updatedAt = currentTime
            )
            favoritesCollection
                .document(currentUid)
                .collection("routes")
                .document(route.id)
                .set(routeWithCreator)
                .await()
            Log.d("ROUTE_REPO", "Saved favorite: ${route.id} by user: $currentUid")
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
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Route::class.java)
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error getting favorites", e)
            emptyList()
        }
    }

    suspend fun getFavoritesCount(): Int {
        return try {
            val currentUid = uid()
            val snapshot = favoritesCollection
                .document(currentUid)
                .collection("routes")
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error getting favorites count", e)
            0
        }
    }

    // Clear all favorites for the current user
    suspend fun clearAllFavorites(): Boolean {
        return try {
            val currentUid = uid()
            val snapshot = favoritesCollection
                .document(currentUid)
                .collection("routes")
                .get()
                .await()

            // Delete each document in the collection
            for (document in snapshot.documents) {
                document.reference.delete().await()
            }

            Log.d("ROUTE_REPO", "Cleared all favorites for user: $currentUid")
            true
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error clearing favorites", e)
            false
        }
    }

    //fetch routeById
    suspend fun getCurrentUserFavoriteRouteById(routeId: String): Route? {
        val currentUid = try {
            uid()
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "User not logged in", e)
            return null
        }
        return try {
            val doc = favoritesCollection
                .document(currentUid)
                .collection("routes")
                .document(routeId)
                .get()
                .await()
            doc.toObject(Route::class.java)
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error getting favorite route", e)
            null
        }
    }

    //swap tag and latlng
    suspend fun swap(routeId: String, oldTag: String, newTag: String, newLatLng: LatLng): Boolean {
        return try {
            val currentUid = uid()
            val docRef = favoritesCollection
                .document(currentUid)
                .collection("routes")
                .document(routeId)

            val document = docRef.get().await()
            
            if (!document.exists()) {
                Log.e("ROUTE_REPO", "Route does not exist: $routeId")
                return false
            }
            
            //pull old arrays
            val oldTags = document.get("tags") as? List<String>
            val oldWaypoints = document.get("waypoints") as? List<GeoPoint>

            if (oldTags == null || oldWaypoints == null) {
                Log.e("ROUTE_REPO", "No tags or waypoints found in route")
                return false
            }

            Log.d("ROUTE_REPO", "Original tags: $oldTags")

            // create copies
            val updatedTags = oldTags.toMutableList()
            val updatedWaypoints = oldWaypoints.toMutableList()

            // find index of oldTag, replace
            val index = updatedTags.indexOf(oldTag)
            if (index == -1) {
                Log.e("ROUTE_REPO", "Tag not found: $oldTag")
                return false
            }

            //convert newLatLng to GeoPoint
            val newGeoPoint = GeoPoint(newLatLng.latitude, newLatLng.longitude)
            updatedWaypoints[index] = newGeoPoint

            //newTag = index of oldtag
            updatedTags[index] = newTag

            // Update waypoints and tags
            docRef.update("waypoints", updatedWaypoints).await()
            docRef.update("tags", updatedTags).await()
            
            // update description to show that it was tweaked by the user
            val newDescription = "User created: ${updatedTags.joinToString(", ")}"
            docRef.update("description", newDescription).await()
            
            Log.d("ROUTE_REPO", "Successfully swapped $oldTag -> $newTag")
            true
        } catch (e: Exception) {
            Log.e("ROUTE_REPO", "Error swapping tag", e)
            false
        }
    }



}