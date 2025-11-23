package moe.group13.routenode.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import moe.group13.routenode.data.model.UserPin

class UserPinRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val userPinsCollection = db.collection("user_pins")

    private fun uid(): String {
        return auth.currentUser?.uid ?: throw Exception("User not logged in")
    }

    suspend fun saveUserPin(userPin: UserPin): String? {
        return try {
            val docRef = userPinsCollection.add(userPin).await()
            Log.d("USER_PIN_REPO", "Saved user pin: ${docRef.id}")
            docRef.id
        } catch (e: Exception) {
            Log.e("USER_PIN_REPO", "Error saving user pin", e)
            null
        }
    }

    suspend fun getAllUserPins(): List<UserPin> {
        return try {
            userPinsCollection
                .get()
                .await()
                .toObjects(UserPin::class.java)
        } catch (e: Exception) {
            Log.e("USER_PIN_REPO", "Error getting user pins", e)
            emptyList()
        }
    }

    suspend fun getUserPinsByUsername(userName: String): List<UserPin> {
        return try {
            userPinsCollection
                .whereEqualTo("userName", userName)
                .get()
                .await()
                .toObjects(UserPin::class.java)
        } catch (e: Exception) {
            Log.e("USER_PIN_REPO", "Error getting user pins by username", e)
            emptyList()
        }
    }

    suspend fun getUserPinById(pinId: String): UserPin? {
        return try {
            val doc = userPinsCollection
                .document(pinId)
                .get()
                .await()
            
            if (doc.exists()) {
                doc.toObject(UserPin::class.java)?.copy(id = doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("USER_PIN_REPO", "Error getting user pin by id", e)
            null
        }
    }

    suspend fun deleteUserPin(pinId: String): Boolean {
        return try {
            userPinsCollection
                .document(pinId)
                .delete()
                .await()
            Log.d("USER_PIN_REPO", "Deleted user pin: $pinId")
            true
        } catch (e: Exception) {
            Log.e("USER_PIN_REPO", "Error deleting user pin", e)
            false
        }
    }

    suspend fun updateUserPin(pinId: String, userPin: UserPin): Boolean {
        return try {
            userPinsCollection
                .document(pinId)
                .set(userPin)
                .await()
            Log.d("USER_PIN_REPO", "Updated user pin: $pinId")
            true
        } catch (e: Exception) {
            Log.e("USER_PIN_REPO", "Error updating user pin", e)
            false
        }
    }
}

