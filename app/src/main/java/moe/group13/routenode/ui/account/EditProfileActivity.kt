package moe.group13.routenode.ui.account

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import moe.group13.routenode.R

class EditProfileActivity : AppCompatActivity() {

    private lateinit var inputName: EditText
    private lateinit var inputBio: EditText
    private lateinit var btnSave: Button
    private lateinit var profileImage: ImageView
    private lateinit var btnChangePhoto: Button

    private var selectedPhotoUri: Uri? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            Glide.with(this).load(uri).into(profileImage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        initViews()
        loadExistingData()

        btnSave.setOnClickListener { saveProfile() }
        btnChangePhoto.setOnClickListener { pickImageLauncher.launch("image/*") }
    }

    private fun initViews() {
        inputName = findViewById(R.id.inputName)
        inputBio = findViewById(R.id.inputBio)
        btnSave = findViewById(R.id.btnSaveProfile)
        profileImage = findViewById(R.id.editProfileImage)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
    }

    private fun loadExistingData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("profile")
            .document("info")
            .get()
            .addOnSuccessListener { doc ->
                inputName.setText(doc.getString("name") ?: "")
                inputBio.setText(doc.getString("bio") ?: "")

                val photoUrl = doc.getString("photoUrl")
                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(this).load(photoUrl).into(profileImage)
                }
            }
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return

        if (selectedPhotoUri != null) {
            uploadPhotoThenSave(uid)
        } else {
            saveToFirestore(uid, null)
        }
    }

    private fun uploadPhotoThenSave(uid: String) {
        val storageRef = storage.getReference("profile_images/$uid.jpg")

        storageRef.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    saveToFirestore(uid, downloadUrl.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToFirestore(uid: String, photoUrl: String?) {
        val data = mutableMapOf(
            "name" to inputName.text.toString(),
            "bio" to inputBio.text.toString()
        )

        if (photoUrl != null)
            data["photoUrl"] = photoUrl

        db.collection("users")
            .document(uid)
            .collection("profile")
            .document("info")
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                goBackToMyProfile()
            }
    }

    private fun goBackToMyProfile() {
        val intent = Intent(this, MyProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
