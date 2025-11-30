package moe.group13.routenode.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.ClickableSpan
import android.text.method.LinkMovementMethod
import android.graphics.Typeface
import android.graphics.Color
import android.content.res.Configuration
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import moe.group13.routenode.MainActivity
import moe.group13.routenode.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // Redirect to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val isDarkTheme = (resources.configuration.uiMode and 
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDarkTheme) Color.WHITE else Color.BLACK

        binding.SignUpTV.setTextColor(textColor)
        binding.forgotPasswordTV.setTextColor(textColor)
        
        val buttonBackgroundColor = if (isDarkTheme) Color.GRAY else Color.BLACK
        binding.SignInBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(buttonBackgroundColor)

        val signUpText = "Not Registered Yet, Sign Up!"
        val spannableSignUp = SpannableString(signUpText)
        val signUpStart = signUpText.indexOf("Sign Up")
        val signUpEnd = signUpStart + "Sign Up".length
        
        spannableSignUp.setSpan(StyleSpan(Typeface.BOLD), signUpStart, signUpEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableSignUp.setSpan(UnderlineSpan(), signUpStart, signUpEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@SignInActivity, SignUpActivity::class.java)
                startActivity(intent)
            }
        }
        spannableSignUp.setSpan(clickableSpan, signUpStart, signUpEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        binding.SignUpTV.text = spannableSignUp
        binding.SignUpTV.movementMethod = LinkMovementMethod.getInstance()

        binding.forgotPasswordTV.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

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

    }
}