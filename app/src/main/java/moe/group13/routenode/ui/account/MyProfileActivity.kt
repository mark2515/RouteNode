package moe.group13.routenode.ui.account

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import moe.group13.routenode.R

class MyProfileActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvCuisine: TextView
    private lateinit var tvTravelMode: TextView
    private lateinit var tvDistance: TextView
    private lateinit var profileImage: ImageView
    private lateinit var btnEditProfile: Button
    private lateinit var btnChangePassword: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        initViews()
        loadProfileData()
        setupButtons()
    }

    private fun initViews() {
        profileImage = findViewById(R.id.profileImage)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvCuisine = findViewById(R.id.tvCuisine)
        tvTravelMode = findViewById(R.id.tvTravelMode)
        tvDistance = findViewById(R.id.tvDistance)

        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnChangePassword = findViewById(R.id.btnChangePassword)
    }

    private fun setupButtons() {
        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
    }

    private fun loadProfileData() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        tvEmail.text = user.email ?: "No Email"

        db.collection("users")
            .document(uid)
            .collection("profile")
            .document("info")
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Unknown User"
                val cuisine = doc.getString("cuisine") ?: "Not Set"
                val travelMode = doc.getString("travelMode") ?: "Not Set"
                val distance = doc.getString("distance") ?: "Not Set"
                val photoUrl = doc.getString("photoUrl")

                tvName.text = name
                tvCuisine.text = cuisine
                tvTravelMode.text = travelMode
                tvDistance.text = distance

                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .into(profileImage)
                }
            }
    }
}
