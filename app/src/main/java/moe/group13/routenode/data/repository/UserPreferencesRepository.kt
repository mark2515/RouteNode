package moe.group13.routenode.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import moe.group13.routenode.data.model.PreferredPlace
import moe.group13.routenode.data.model.UserPreferences

class UserPreferencesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val preferencesCollection = db.collection("user_preferences")

    private fun uid(): String {
        return auth.currentUser?.uid ?: throw Exception("User not logged in")
    }

    // Get user preferences
    suspend fun getPreferences(uid: String? = null): UserPreferences? {
        return try {
            val targetUid = uid ?: uid()
            val doc = preferencesCollection
                .document(targetUid)
                .get()
                .await()

            if (doc.exists()) {
                doc.toObject(UserPreferences::class.java)?.copy(id = doc.id, userId = targetUid)
            } else {
                // Create default preferences if they don't exist
                val defaultPrefs = UserPreferences(userId = targetUid)
                savePreferences(defaultPrefs)
                defaultPrefs
            }
        } catch (e: Exception) {
            Log.e("PREF_REPO", "Error getting preferences", e)
            null
        }
    }

    // Save or update user preferences
    suspend fun savePreferences(preferences: UserPreferences): Boolean {
        return try {
            val currentUid = uid()
            val prefsWithUser = preferences.copy(
                userId = currentUid,
                updatedAt = System.currentTimeMillis()
            )
            preferencesCollection
                .document(currentUid)
                .set(prefsWithUser)
                .await()
            Log.d("PREF_REPO", "Saved preferences for user: $currentUid")
            true
        } catch (e: Exception) {
            Log.e("PREF_REPO", "Error saving preferences", e)
            false
        }
    }

    // Update specific preference fields
    suspend fun updatePreferences(
        favoriteCuisines: List<String>? = null,
        travelMode: String? = null,
        preferredDistanceKm: Double? = null,
        unitPreference: String? = null,
        theme: String? = null,
        notificationsEnabled: Boolean? = null,
        routeUpdatesEnabled: Boolean? = null
    ): Boolean {
        return try {
            val currentUid = uid()
            val updates = mutableMapOf<String, Any>("updatedAt" to System.currentTimeMillis())

            favoriteCuisines?.let { updates["favoriteCuisines"] = it }
            travelMode?.let { updates["travelMode"] = it }
            preferredDistanceKm?.let { updates["preferredDistanceKm"] = it }
            unitPreference?.let { updates["unitPreference"] = it }
            theme?.let { updates["theme"] = it }
            notificationsEnabled?.let { updates["notificationsEnabled"] = it }
            routeUpdatesEnabled?.let { updates["routeUpdatesEnabled"] = it }

            preferencesCollection
                .document(currentUid)
                .update(updates)
                .await()
            Log.d("PREF_REPO", "Updated preferences for user: $currentUid")
            true
        } catch (e: Exception) {
            Log.e("PREF_REPO", "Error updating preferences", e)
            false
        }
    }

    // Preferred Places Management
    suspend fun addPreferredPlace(place: PreferredPlace): Boolean {
        return try {
            val currentUid = uid()
            val prefs = getPreferences() ?: UserPreferences(userId = currentUid)
            val updatedPlaces = prefs.preferredPlaces + place
            val updatedPrefs = prefs.copy(preferredPlaces = updatedPlaces)
            savePreferences(updatedPrefs)
            Log.d("PREF_REPO", "Added preferred place: ${place.name}")
            true
        } catch (e: Exception) {
            Log.e("PREF_REPO", "Error adding preferred place", e)
            false
        }
    }

    suspend fun removePreferredPlace(placeName: String): Boolean {
        return try {
            val prefs = getPreferences() ?: return false
            val updatedPlaces = prefs.preferredPlaces.filter { it.name != placeName }
            val updatedPrefs = prefs.copy(preferredPlaces = updatedPlaces)
            savePreferences(updatedPrefs)
            Log.d("PREF_REPO", "Removed preferred place: $placeName")
            true
        } catch (e: Exception) {
            Log.e("PREF_REPO", "Error removing preferred place", e)
            false
        }
    }

    suspend fun updatePreferredPlace(placeName: String, updatedPlace: PreferredPlace): Boolean {
        return try {
            val prefs = getPreferences() ?: return false
            val updatedPlaces = prefs.preferredPlaces.map {
                if (it.name == placeName) updatedPlace else it
            }
            val updatedPrefs = prefs.copy(preferredPlaces = updatedPlaces)
            savePreferences(updatedPrefs)
            Log.d("PREF_REPO", "Updated preferred place: $placeName")
            true
        } catch (e: Exception) {
            Log.e("PREF_REPO", "Error updating preferred place", e)
            false
        }
    }

    suspend fun getPreferredPlaces(): List<PreferredPlace> {
        return try {
            val prefs = getPreferences()
            prefs?.preferredPlaces ?: emptyList()
        } catch (e: Exception) {
            Log.e("PREF_REPO", "Error getting preferred places", e)
            emptyList()
        }
    }
}

