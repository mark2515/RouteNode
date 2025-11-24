package moe.group13.routenode.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import moe.group13.routenode.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchRecommendations: Switch
    private lateinit var switchUpdates: Switch
    private lateinit var spinnerTheme: Spinner
    private lateinit var spinnerUnits: Spinner
    private lateinit var btnClearHistory: Button
    private lateinit var btnPermissions: Button
    private lateinit var btnHelp: Button
    private lateinit var btnAbout: Button

    private val PREFS = "route_settings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved theme BEFORE setContentView
        applySavedTheme()

        setContentView(R.layout.activity_settings)

        // Bind UI
        switchRecommendations = findViewById(R.id.switchRecommendations)
        switchUpdates = findViewById(R.id.switchUpdates)
        spinnerTheme = findViewById(R.id.spinnerTheme)
        spinnerUnits = findViewById(R.id.spinnerUnits)
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

        btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear search & route history?")
                .setPositiveButton("Yes") { _, _ ->
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Help & FAQ coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnAbout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("About RouteNode")
                .setMessage("RouteNode version 1.0.0\nDeveloped by Group 13")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // ------------------------------------------------
    // Theme Logic
    // ------------------------------------------------
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
}

