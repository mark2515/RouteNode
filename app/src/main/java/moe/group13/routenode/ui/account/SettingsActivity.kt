package moe.group13.routenode.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import moe.group13.routenode.R
import moe.group13.routenode.data.repository.RouteRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchRecommendations: Switch
    private lateinit var switchUpdates: Switch
    private lateinit var spinnerTheme: Spinner
    private lateinit var spinnerUnits: Spinner
    private lateinit var layoutCommonLocations: LinearLayout
    private lateinit var layoutCommonPlaces: LinearLayout
    private lateinit var btnClearHistory: Button
    private lateinit var btnPermissions: Button
    private lateinit var btnHelp: Button
    private lateinit var btnAbout: Button

    private val PREFS = "route_settings"
    private val routeRepository = RouteRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applySavedTheme()

        setContentView(R.layout.activity_settings)

        setupTopAppBar()

        // Bind UI
        switchRecommendations = findViewById(R.id.switchRecommendations)
        switchUpdates = findViewById(R.id.switchUpdates)
        spinnerTheme = findViewById(R.id.spinnerTheme)
        spinnerUnits = findViewById(R.id.spinnerUnits)
        layoutCommonLocations = findViewById(R.id.layoutCommonLocations)
        layoutCommonPlaces = findViewById(R.id.layoutCommonPlaces)
        btnClearHistory = findViewById(R.id.btnClearHistory)
        btnPermissions = findViewById(R.id.btnPermissions)
        btnHelp = findViewById(R.id.btnHelp)
        btnAbout = findViewById(R.id.btnAbout)

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Load settings
        switchRecommendations.isChecked = prefs.getBoolean("recommendations", true)
        switchUpdates.isChecked = prefs.getBoolean("updates", true)
        spinnerTheme.setSelection(prefs.getInt("theme_index", 0))
        spinnerUnits.setSelection(prefs.getInt("unit_index", 0))

        switchRecommendations.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("recommendations", isChecked).apply()
        }

        switchUpdates.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("updates", isChecked).apply()
        }

        // THEME CHANGE HANDLER
        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View?,
                position: Int, id: Long
            ) {
                prefs.edit().putInt("theme_index", position).apply()
                updateTheme(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerUnits.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View?,
                position: Int, id: Long
            ) {
                prefs.edit().putInt("unit_index", position).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        layoutCommonLocations.setOnClickListener {
            val intent = Intent(this, CommonLocationsActivity::class.java)
            startActivity(intent)
        }

        layoutCommonPlaces.setOnClickListener {
            val intent = Intent(this, CommonPlacesActivity::class.java)
            startActivity(intent)
        }

        btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all favorite routes? This action cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    clearAllFavoriteRoutes()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnPermissions.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }

        btnHelp.setOnClickListener {
            showHelpAndFaqDialog()
        }

        btnAbout.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://route-node-webpage.vercel.app/"))
            startActivity(intent)
        }
    }

    private fun setupTopAppBar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            // Return to the Account screen
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun updateTheme(index: Int) {
        when (index) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val index = prefs.getInt("theme_index", 0)
        updateTheme(index)
    }

    private fun clearAllFavoriteRoutes() {
        lifecycleScope.launch {
            try {
                val success = routeRepository.clearAllFavorites()
                if (success) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "All favorite routes have been cleared",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Failed to clear favorites. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showHelpAndFaqDialog() {
        val faqContent = """
            <b>Q: How is your app's place recommendation different from Google Maps?</b>
            <br/><br/>
            A: Google Maps shows places based on real-world data like user reviews, ratings, and popularity. Our AI, on the other hand, uses semantic reasoning from the information it was trained on, so it's better at understanding vague requests like "a library where I can plug in my laptop." It can also help plan routes in a more flexible, natural way.
            <br/><br/><br/>
            <b>Q: If your app uses a large language model, why can't I just ask ChatGPT directly?</b>
            <br/><br/>
            A: You can, but the process is pretty complicated. If you want to visit multiple places, you have to give ChatGPT the exact address for every stop, and you also need to phrase your request very clearly so it understands. Our app makes this much easier. With Google Maps Place Autocomplete, you only need to enter part of the address. You just fill out a simple form, so you don't need perfect wording, and the AI gives you a clean, structured answer. You can also save your favorite routes and jump straight into Google Maps navigation with one tap.
        """.trimIndent()

        val scrollView = ScrollView(this)
        val textView = TextView(this)
        textView.text = Html.fromHtml(faqContent, Html.FROM_HTML_MODE_LEGACY)
        textView.textSize = 16f
        textView.setPadding(60, 40, 60, 40)
        textView.setTextIsSelectable(true)
        
        scrollView.addView(textView)
        
        AlertDialog.Builder(this)
            .setTitle("Help & FAQ")
            .setView(scrollView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}