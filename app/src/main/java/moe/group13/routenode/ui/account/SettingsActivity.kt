package moe.group13.routenode.ui.account

import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import moe.group13.routenode.R

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var viewModel: UserPreferencesViewModel
    private lateinit var switchRecommendations: Switch
    private lateinit var switchUpdates: Switch
    private lateinit var spinnerTheme: Spinner
    private lateinit var spinnerUnits: Spinner
    private lateinit var btnSave: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        viewModel = ViewModelProvider(this)[UserPreferencesViewModel::class.java]
        
        initViews()
        loadSettings()
        setupListeners()
        observeViewModel()
    }
    
    private fun initViews() {
        switchRecommendations = findViewById(R.id.switchRecommendations)
        switchUpdates = findViewById(R.id.switchUpdates)
        spinnerTheme = findViewById(R.id.spinnerTheme)
        spinnerUnits = findViewById(R.id.spinnerUnits)
        btnSave = findViewById(R.id.btnSaveSettings)
    }
    
    private fun loadSettings() {
        viewModel.loadPreferences()
    }
    
    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun observeViewModel() {
        viewModel.preferences.observe(this) { preferences ->
            preferences?.let {
                // Load notification settings
                switchRecommendations.isChecked = it.notificationsEnabled
                switchUpdates.isChecked = it.routeUpdatesEnabled
                
                // Load theme setting
                val themeOptions = resources.getStringArray(R.array.theme_options)
                val themeIndex = when (it.theme) {
                    "light" -> themeOptions.indexOf("Light")
                    "dark" -> themeOptions.indexOf("Dark")
                    else -> themeOptions.indexOf("System Default")
                }
                if (themeIndex >= 0) {
                    spinnerTheme.setSelection(themeIndex)
                }
                
                // Load unit preference
                val unitOptions = resources.getStringArray(R.array.unit_options)
                val unitIndex = when (it.unitPreference) {
                    "miles" -> unitOptions.indexOf("Miles")
                    else -> unitOptions.indexOf("Kilometers")
                }
                if (unitIndex >= 0) {
                    spinnerUnits.setSelection(unitIndex)
                }
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            btnSave.isEnabled = !isLoading
            btnSave.text = if (isLoading) "Saving..." else "Save Settings"
        }
        
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun saveSettings() {
        val currentPrefs = viewModel.preferences.value ?: return
        
        // Get theme selection
        val themeOptions = resources.getStringArray(R.array.theme_options)
        val selectedTheme = when (spinnerTheme.selectedItemPosition) {
            themeOptions.indexOf("Light") -> "light"
            themeOptions.indexOf("Dark") -> "dark"
            else -> "system"
        }
        
        // Get unit selection
        val unitOptions = resources.getStringArray(R.array.unit_options)
        val selectedUnit = when (spinnerUnits.selectedItemPosition) {
            unitOptions.indexOf("Miles") -> "miles"
            else -> "km"
        }
        
        // Update preferences
        val updatedPrefs = currentPrefs.copy(
            theme = selectedTheme,
            unitPreference = selectedUnit,
            notificationsEnabled = switchRecommendations.isChecked,
            routeUpdatesEnabled = switchUpdates.isChecked,
            updatedAt = System.currentTimeMillis()
        )
        
        viewModel.savePreferences(updatedPrefs)
        
        // Show success message
        Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
    }
}