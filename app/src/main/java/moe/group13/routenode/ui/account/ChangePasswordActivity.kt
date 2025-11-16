package moe.group13.routenode.ui.account

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import moe.group13.routenode.R

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var oldPassword: EditText
    private lateinit var newPassword: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var updateBtn: Button

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        oldPassword = findViewById(R.id.inputOldPassword)
        newPassword = findViewById(R.id.inputNewPassword)
        confirmPassword = findViewById(R.id.inputConfirmPassword)
        updateBtn = findViewById(R.id.btnUpdatePassword)

        updateBtn.setOnClickListener { updatePassword() }
    }

    private fun updatePassword() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val email = user.email ?: return
        val oldPass = oldPassword.text.toString()
        val newPass = newPassword.text.toString()
        val confirm = confirmPassword.text.toString()

        if (oldPass.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass != confirm) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = EmailAuthProvider.getCredential(email, oldPass)

        user.reauthenticate(credential).addOnSuccessListener {
            user.updatePassword(newPass).addOnSuccessListener {
                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_LONG).show()
                finish()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show()
        }
    }
}
