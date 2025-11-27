package moe.group13.routenode.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.ClickableSpan
import android.text.method.LinkMovementMethod
import android.graphics.Typeface
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import moe.group13.routenode.MainActivity
import moe.group13.routenode.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val signInText = "Already Registered, Sign In!"
        val spannableSignIn = SpannableString(signInText)
        val signInStart = signInText.indexOf("Sign In")
        val signInEnd = signInStart + "Sign In".length
        
        spannableSignIn.setSpan(StyleSpan(Typeface.BOLD), signInStart, signInEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableSignIn.setSpan(UnderlineSpan(), signInStart, signInEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@SignUpActivity, SignInActivity::class.java)
                startActivity(intent)
            }
        }
        spannableSignIn.setSpan(clickableSpan, signInStart, signInEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        binding.SignInTV.text = spannableSignIn
        binding.SignInTV.movementMethod = LinkMovementMethod.getInstance()

        binding.SignUpBtn.setOnClickListener {
            val email = binding.EmailET.text.toString()
            val password = binding.PasswordET.text.toString()
            val confirmPassword = binding.ConfirmPasswordET.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Passwords do not match. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty fields are not allowed. Please fill out all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}