package moe.group13.routenode.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyProfileViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    val user = auth.currentUser

    fun loadProfile(onResult: (String?, String?, String?) -> Unit) {
        val uid = user?.uid ?: return onResult(null, null, null)

        db.collection("users")
            .document(uid)
            .collection("profile")
            .document("info")
            .get()
            .addOnSuccessListener { doc ->
                onResult(
                    doc.getString("name"),
                    doc.getString("bio"),
                    doc.getString("photoUrl")
                )
            }
            .addOnFailureListener {
                onResult(null, null, null)
            }
    }

    fun saveProfile(name: String, bio: String, photoUrl: String?, onDone: () -> Unit) {
        val uid = user?.uid ?: return

        viewModelScope.launch {
            val data = mapOf(
                "name" to name,
                "bio" to bio,
                "photoUrl" to photoUrl,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(uid)
                .collection("profile")
                .document("info")
                .set(data)
                .await()

            onDone()
        }
    }
}
