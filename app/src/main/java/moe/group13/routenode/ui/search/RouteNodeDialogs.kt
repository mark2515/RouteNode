package moe.group13.routenode.ui.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import moe.group13.routenode.R
import moe.group13.routenode.ui.account.LocationItem

object RouteNodeDialogs {

    fun showDeleteConfirmation(
        context: Context,
        nodeNo: Int,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Delete Route Node")
            .setMessage("Are you sure you want to delete Route Node No. $nodeNo?")
            .setPositiveButton("Confirm") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    fun showMoreOptionsLocation(
        context: Context,
        onLocationSelected: (String) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                context,
                "Please sign in to use common locations.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(user.uid)
            .collection("saved_locations")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val locations = snapshot?.documents?.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val address = doc.getString("address") ?: return@mapNotNull null
                    LocationItem(name = name, address = address)
                } ?: emptyList()

                val builder = AlertDialog.Builder(context)
                    .setTitle("Use Common Locations")

                val inflater = LayoutInflater.from(context)
                val dialogView = inflater.inflate(R.layout.dialog_common_locations, null)
                val container =
                    dialogView.findViewById<LinearLayout>(R.id.containerLocations)

                var dialog: AlertDialog? = null
                var selectedAddress: String? = null
                var selectedView: View? = null

                if (locations.isEmpty()) {
                    val emptyView = TextView(context).apply {
                        text = "You don't have any saved locations yet."
                        textSize = 14f
                        setTextColor(0xFF555555.toInt())
                        setPadding(8, 8, 8, 8)
                    }
                    container.addView(emptyView)
                } else {
                    locations.forEach { locationItem ->
                        val itemView = inflater.inflate(
                            R.layout.item_common_location_card,
                            container,
                            false
                        ) as View

                        val nameText =
                            itemView.findViewById<TextView>(R.id.txtLocationName)
                        val addressText =
                            itemView.findViewById<TextView>(R.id.txtLocationAddress)

                        nameText.text = locationItem.name
                        addressText.text = locationItem.address

                        itemView.setOnClickListener {
                            // Update selected state
                            selectedView?.setBackgroundResource(R.drawable.bg_input_rect)
                            itemView.setBackgroundResource(R.drawable.bg_input_rect_selected)
                            selectedView = itemView
                            selectedAddress = locationItem.address
                        }

                        container.addView(itemView)
                    }
                }

                builder.setView(dialogView)
                    .setNegativeButton("Cancel") { d, _ ->
                        d.dismiss()
                    }
                    .setPositiveButton("OK") { d, _ ->
                        val address = selectedAddress
                        if (address != null) {
                            onLocationSelected(address)
                        } else {
                            Toast.makeText(
                                context,
                                "Please select a location first.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        d.dismiss()
                    }

                dialog = builder.create()
                dialog.show()
            }
            .addOnFailureListener {
                AlertDialog.Builder(context)
                    .setTitle("Use Common Locations")
                    .setMessage("Failed to load common locations. Please try again.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
    }

    fun showMoreOptionsPlace(
        context: Context,
        onPlaceSelected: (String) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                context,
                "Please sign in to use common places.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(user.uid)
            .collection("saved_places")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val places = snapshot?.documents?.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    LocationItem(name = name, address = "")
                } ?: emptyList()

                val builder = AlertDialog.Builder(context)
                    .setTitle("Use Common Places")

                val inflater = LayoutInflater.from(context)
                val dialogView = inflater.inflate(R.layout.dialog_common_locations, null)
                val container =
                    dialogView.findViewById<LinearLayout>(R.id.containerLocations)

                var dialog: AlertDialog? = null
                var selectedPlaceName: String? = null
                var selectedView: View? = null

                if (places.isEmpty()) {
                    val emptyView = TextView(context).apply {
                        text = "You don't have any saved places yet."
                        textSize = 14f
                        setTextColor(0xFF555555.toInt())
                        setPadding(8, 8, 8, 8)
                    }
                    container.addView(emptyView)
                } else {
                    places.forEach { placeItem ->
                        val itemView = inflater.inflate(
                            R.layout.item_common_location_card,
                            container,
                            false
                        ) as View

                        val nameText =
                            itemView.findViewById<TextView>(R.id.txtLocationName)
                        val addressText =
                            itemView.findViewById<TextView>(R.id.txtLocationAddress)

                        nameText.text = placeItem.name
                        addressText.text = ""

                        itemView.setOnClickListener {
                            selectedView?.setBackgroundResource(R.drawable.bg_input_rect)
                            itemView.setBackgroundResource(R.drawable.bg_input_rect_selected)
                            selectedView = itemView
                            selectedPlaceName = placeItem.name
                        }

                        container.addView(itemView)
                    }
                }

                builder.setView(dialogView)
                    .setNegativeButton("Cancel") { d, _ ->
                        d.dismiss()
                    }
                    .setPositiveButton("OK") { d, _ ->
                        val placeName = selectedPlaceName
                        if (placeName != null) {
                            onPlaceSelected(placeName)
                        } else {
                            Toast.makeText(
                                context,
                                "Please select a place first.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        d.dismiss()
                    }

                dialog = builder.create()
                dialog.show()
            }
            .addOnFailureListener {
                AlertDialog.Builder(context)
                    .setTitle("Use Common Places")
                    .setMessage("Failed to load common places. Please try again.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
    }
}