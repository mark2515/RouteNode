package moe.group13.routenode.ui.account

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import moe.group13.routenode.R
import moe.group13.routenode.utils.GptConfig
import kotlin.math.roundToInt

class AIModelsActivity : AppCompatActivity() {

    companion object {
        private const val PREFS = "ai_model_settings"
        private const val KEY_MODEL = "model"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_TOKENS = "max_tokens"

        private val AVAILABLE_MODELS = listOf(
            "gpt-4-turbo",
            "gpt-4.1",
            "gpt-4.1-mini"
        )

        private val DEFAULT_MODEL = GptConfig.DEFAULT_CONFIG.model
        private const val DEFAULT_LANGUAGE = "English"
        private val DEFAULT_TEMPERATURE = GptConfig.DEFAULT_CONFIG.temperature
        private val DEFAULT_MAX_TOKENS = GptConfig.DEFAULT_CONFIG.max_tokens

        private const val MIN_TEMPERATURE = 0.2
        private const val MAX_TEMPERATURE = 1.2
        private const val TEMPERATURE_STEP = 0.1

        private const val MIN_TOKENS = 400
        private const val MAX_TOKENS = 1200
        private const val TOKENS_STEP = 100
    }

    private val languages = listOf(
        "English",
        "中文",
        "Español",
        "Français",
        "العربية",
        "Русский",
        "Deutsch",
        "Português",
        "日本語",
        "हिन्दी"
    )

    private lateinit var spinnerModel: Spinner
    private lateinit var spinnerResponseLanguage: Spinner
    private lateinit var editCreativity: EditText
    private lateinit var btnCreativityUp: ImageButton
    private lateinit var btnCreativityDown: ImageButton
    private lateinit var editResponseLength: EditText
    private lateinit var btnResponseLengthUp: ImageButton
    private lateinit var btnResponseLengthDown: ImageButton
    private lateinit var btnSaveChanges: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_models)

        initViews()
        setupModelSpinner()
        setupLanguageSpinner()
        loadSavedValues()
        setupButtons()
    }

    private fun initViews() {
        spinnerModel = findViewById(R.id.spinnerModel)
        spinnerResponseLanguage = findViewById(R.id.spinnerResponseLanguage)
        editCreativity = findViewById(R.id.editCreativity)
        btnCreativityUp = findViewById(R.id.btnCreativityUp)
        btnCreativityDown = findViewById(R.id.btnCreativityDown)
        editResponseLength = findViewById(R.id.editResponseLength)
        btnResponseLengthUp = findViewById(R.id.btnResponseLengthUp)
        btnResponseLengthDown = findViewById(R.id.btnResponseLengthDown)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
    }

    private fun setupModelSpinner() {
        val models = AVAILABLE_MODELS
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            models
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModel.adapter = adapter
    }

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languages
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerResponseLanguage.adapter = adapter
    }

    private fun loadSavedValues() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        val savedModel = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        val savedLanguage = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        val savedTemperature = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE.toFloat())
            .toDouble()
        val savedMaxTokens = prefs.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)
        val modelIndex = AVAILABLE_MODELS.indexOf(savedModel).let { index ->
            if (index >= 0) {
                index
            } else {
                val defaultIndex = AVAILABLE_MODELS.indexOf(DEFAULT_MODEL)
                if (defaultIndex >= 0) defaultIndex else 0
            }
        }
        spinnerModel.setSelection(modelIndex)

        // Set language selection
        val languageIndex = languages.indexOf(savedLanguage).let { index ->
            if (index >= 0) index else 0
        }
        spinnerResponseLanguage.setSelection(languageIndex)

        val clampedTemp = clampTemperature(savedTemperature)
        val clampedTokens = clampTokens(savedMaxTokens)

        editCreativity.setText(String.format("%.1f", clampedTemp))
        editResponseLength.setText(clampedTokens.toString())
    }

    private fun setupButtons() {
        btnCreativityUp.setOnClickListener {
            adjustCreativity(TEMPERATURE_STEP)
        }

        btnCreativityDown.setOnClickListener {
            adjustCreativity(-TEMPERATURE_STEP)
        }

        btnResponseLengthUp.setOnClickListener {
            adjustResponseLength(TOKENS_STEP)
        }

        btnResponseLengthDown.setOnClickListener {
            adjustResponseLength(-TOKENS_STEP)
        }

        btnSaveChanges.setOnClickListener {
            saveCurrentValues()
            Toast.makeText(
                this@AIModelsActivity,
                "Changes saved",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun adjustCreativity(delta: Double) {
        val current = parseCreativity()
        val newValue = clampTemperature(current + delta)
        editCreativity.setText(String.format("%.1f", newValue))
    }

    private fun adjustResponseLength(delta: Int) {
        val current = parseResponseLength()
        val newValue = clampTokens(current + delta)
        editResponseLength.setText(newValue.toString())
    }

    private fun parseCreativity(): Double {
        val text = editCreativity.text.toString()
        val value = text.toDoubleOrNull() ?: DEFAULT_TEMPERATURE
        val snapped = (value / TEMPERATURE_STEP).roundToInt() * TEMPERATURE_STEP
        return clampTemperature(snapped)
    }

    private fun parseResponseLength(): Int {
        val text = editResponseLength.text.toString()
        val value = text.toIntOrNull() ?: DEFAULT_MAX_TOKENS
        val snapped = ((value.toDouble() / TOKENS_STEP).roundToInt() * TOKENS_STEP)
        return clampTokens(snapped)
    }

    private fun clampTemperature(value: Double): Double {
        return value.coerceIn(MIN_TEMPERATURE, MAX_TEMPERATURE)
    }

    private fun clampTokens(value: Int): Int {
        return value.coerceIn(MIN_TOKENS, MAX_TOKENS)
    }

    private fun saveCurrentValues() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val editor = prefs.edit()

        val model = spinnerModel.selectedItem?.toString() ?: DEFAULT_MODEL
        val language = spinnerResponseLanguage.selectedItem?.toString() ?: DEFAULT_LANGUAGE
        val temperature = parseCreativity()
        val maxTokens = parseResponseLength()

        editor.putString(KEY_MODEL, model)
        editor.putString(KEY_LANGUAGE, language)
        editor.putFloat(KEY_TEMPERATURE, temperature.toFloat())
        editor.putInt(KEY_MAX_TOKENS, maxTokens)
        editor.apply()
    }
}