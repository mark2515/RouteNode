package moe.group13.routenode.ui.account

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import moe.group13.routenode.R

class CommonPlacesActivity : AppCompatActivity() {

    private lateinit var editPlace: EditText
    private lateinit var buttonSavePlace: Button
    private lateinit var tvNoPlaces: TextView
    private lateinit var recyclerPlaces: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_places)

        setupTopAppBar()
        initViews()
        setupSaveButton()
    }

    private fun setupTopAppBar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initViews() {
        editPlace = findViewById(R.id.editPlace)
        buttonSavePlace = findViewById(R.id.buttonSavePlace)
        tvNoPlaces = findViewById(R.id.tvNoPlaces)
        recyclerPlaces = findViewById(R.id.recyclerPlaces)
    }

    private fun setupSaveButton() {
        buttonSavePlace.setOnClickListener {
            val place = editPlace.text.toString().trim()

            when {
                place.isEmpty() -> {
                    Toast.makeText(this, "Please enter a place", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Save place logic will be implemented here
                    Toast.makeText(
                        this,
                        "Place saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Clear field
                    editPlace.text.clear()
                }
            }
        }
    }
}