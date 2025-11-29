package moe.group13.routenode.ui.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SettingsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    val savedLocations = MutableLiveData<List<LocationItem>>()

    fun saveLocation(address: String, name: String) {
        if (uid == null) return

        val data = hashMapOf(
            "name" to name,
            "address" to address,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(uid)
            .collection("saved_locations")
            .add(data)
    }

    fun loadLocations() {
        if (uid == null) return

        firestore.collection("users")
            .document(uid)
            .collection("saved_locations")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.map {
                    LocationItem(
                        name = it.getString("name") ?: "",
                        address = it.getString("address") ?: ""
                    )
                } ?: emptyList()

                savedLocations.value = list
            }
    }
}

data class LocationItem(
    val name: String,
    val address: String
)