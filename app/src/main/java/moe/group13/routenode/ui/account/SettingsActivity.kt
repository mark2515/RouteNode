package moe.group13.routenode.ui.account

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Html
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
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
    private val LOCATION_REQUEST_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
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

                if (position == 3) {
                    ensureLocationPermissionAndApplyAuto()
                } else {
                    updateTheme(position)
                }
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
            startActivity(Intent(this, CommonLocationsActivity::class.java))
        }

        layoutCommonPlaces.setOnClickListener {
            startActivity(Intent(this, CommonPlacesActivity::class.java))
        }

        btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all favorite routes? This action cannot be undone.")
                .setPositiveButton("Yes") { _, _ -> clearAllFavoriteRoutes() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnPermissions.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }

        btnHelp.setOnClickListener { showHelpAndFaqDialog() }

        btnAbout.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://route-node-webpage.vercel.app/"))
            startActivity(intent)
        }
    }

    private fun setupTopAppBar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun updateTheme(index: Int) {
        when (index) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            3 -> ThemeManager.applyAutoTheme(this)
        }
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val index = prefs.getInt("theme_index", 0)

        if (index == 3) {
            ThemeManager.applyAutoTheme(this)
        } else {
            updateTheme(index)
        }
    }

    private fun ensureLocationPermissionAndApplyAuto() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION

        val hasPermission =
            ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            ThemeManager.applyAutoTheme(this)
        } else {
            requestPermissions(arrayOf(fine, coarse), LOCATION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            if (granted) {
                ThemeManager.applyAutoTheme(this)
            } else {
                Toast.makeText(this, "Location permission required for Auto theme", Toast.LENGTH_SHORT).show()
                spinnerTheme.setSelection(0) // fallback
            }
        }
    }

    private fun clearAllFavoriteRoutes() {
        lifecycleScope.launch {
            try {
                val success = routeRepository.clearAllFavorites()
                Toast.makeText(
                    this@SettingsActivity,
                    if (success) "All favorite routes have been cleared" else "Failed to clear favorites.",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showHelpAndFaqDialog() {
        val faqContent = """
            <b>Q: How is your app's place recommendation different from Google Maps?</b>
            <br/><br/>
            A: Google Maps shows places based on real-world data like user reviews and popularity. Our AI uses semantic reasoning and understands vague requests like "a quiet cafe to study".
            <br/><br/><br/>
            <b>Q: Why not just ask ChatGPT?</b>
            <br/><br/>
            A: You can — but you'd need to type exact addresses for each stop. Our app combines Google Maps Autocomplete + route logic + instant navigation.
        """.trimIndent()

        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            text = Html.fromHtml(faqContent, Html.FROM_HTML_MODE_LEGACY)
            textSize = 16f
            setPadding(60, 40, 60, 40)
            setTextIsSelectable(true)
        }

        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle("Help & FAQ")
            .setView(scrollView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
