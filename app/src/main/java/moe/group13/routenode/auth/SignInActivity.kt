package moe.group13.routenode.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import moe.group13.routenode.MainActivity
import moe.group13.routenode.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.SignInBtn.setOnClickListener {
            // Disable the button to prevent multiple clicks
            binding.SignInBtn.isEnabled = false
            
            val email = binding.EmailET.text.toString()
            val password = binding.PasswordET.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {

                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Re-enable the button if sign in fails
                        binding.SignInBtn.isEnabled = true
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()

                    }
                }
            } else {
                // Re-enable the button if validation fails
                binding.SignInBtn.isEnabled = true
                Toast.makeText(this, "Empty Fields Are not Allowed!! Please try again.", Toast.LENGTH_SHORT).show()

            }
        }

        binding.SignUpTV.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

    }
}