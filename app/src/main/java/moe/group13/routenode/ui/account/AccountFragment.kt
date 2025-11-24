package moe.group13.routenode.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import moe.group13.routenode.R
import android.content.Intent
import moe.group13.routenode.auth.SignInActivity

class AccountFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var profileImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myProfileBtn = view.findViewById<LinearLayout>(R.id.myProfileBtn)
        val settingsBtn = view.findViewById<LinearLayout>(R.id.SettingsBtn)
        val AIBtn = view.findViewById<LinearLayout>(R.id.AIModelsBtn)
        val logoutBtn = view.findViewById<LinearLayout>(R.id.LogoutBtn)

        nameTextView = view.findViewById(R.id.NameTV)
        emailTextView = view.findViewById(R.id.EmailTV)
        profileImage = view.findViewById(R.id.profileImageView) // If you have one

        loadAccountInfo()

        myProfileBtn.setOnClickListener {
            startActivity(Intent(requireContext(), MyProfileActivity::class.java))
        }

        settingsBtn.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(requireContext(), SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadAccountInfo()
    }

    private fun loadAccountInfo() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        emailTextView.text = user.email ?: "Unknown Email"

        db.collection("users")
            .document(uid)
            .collection("profile")
            .document("info")
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "User"
                val photoUrl = doc.getString("photoUrl")

                nameTextView.text = name

                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(photoUrl)
                        .into(profileImage)
                }
            }
    }
}
