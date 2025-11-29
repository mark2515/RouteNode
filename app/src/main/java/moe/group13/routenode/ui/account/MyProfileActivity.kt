package moe.group13.routenode.ui.account

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import moe.group13.routenode.R

class MyProfileActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPreferredTheme: TextView
    private lateinit var tvPreferredDistance: TextView
    private lateinit var profileImage: ImageView
    private lateinit var btnEditProfile: Button
    private lateinit var btnChangePassword: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        setupTopAppBar()
        initViews()
        loadProfileData()  // initial load
        loadPreferenceSummary()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
        loadPreferenceSummary()
    }

    private fun setupTopAppBar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            // Return to the Account screen
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initViews() {
        profileImage = findViewById(R.id.profileImage)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPreferredTheme = findViewById(R.id.tvPreferredTheme)
        tvPreferredDistance = findViewById(R.id.tvPreferredDistance)

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
                val photoUrl = doc.getString("photoUrl")

                tvName.text = name

                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .into(profileImage)
                }
            }
    }

    private fun loadPreferenceSummary() {
        // Use the same SharedPreferences as SettingsActivity
        val prefs = getSharedPreferences("route_settings", Context.MODE_PRIVATE)

        val themeIndex = prefs.getInt("theme_index", 0)
        val unitIndex = prefs.getInt("unit_index", 0)

        val themeOptions = resources.getStringArray(R.array.theme_options)
        val unitOptions = resources.getStringArray(R.array.unit_options)

        val themeText = themeOptions.getOrNull(themeIndex) ?: themeOptions.firstOrNull() ?: ""
        val unitText = unitOptions.getOrNull(unitIndex) ?: unitOptions.firstOrNull() ?: ""

        tvPreferredTheme.text = themeText
        tvPreferredDistance.text = unitText
    }
}
