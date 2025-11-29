package moe.group13.routenode.ui.account

import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.appbar.MaterialToolbar
import moe.group13.routenode.R
import moe.group13.routenode.ui.search.PlacesAutoCompleteAdapter

class CommonLocationsActivity : AppCompatActivity() {

    private lateinit var editAddress: AutoCompleteTextView
    private lateinit var buttonSaveLocation: Button
    private lateinit var tvNoLocations: TextView
    private lateinit var recyclerLocations: RecyclerView
    
    private lateinit var placesClient: PlacesClient
    private lateinit var placesAdapter: PlacesAutoCompleteAdapter
    
    private var selectedPlace: Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_locations)

        setupTopAppBar()
        initializePlaces()
        initViews()
        setupAddressSearch()
        setupSaveButton()
    }

    private fun setupTopAppBar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initializePlaces() {
        // Initialize Places SDK if not already initialized
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)
    }

    private fun initViews() {
        editAddress = findViewById(R.id.editAddress)
        buttonSaveLocation = findViewById(R.id.buttonSaveLocation)
        tvNoLocations = findViewById(R.id.tvNoLocations)
        recyclerLocations = findViewById(R.id.recyclerLocations)
    }

    private fun setupAddressSearch() {
        // Create Places autocomplete adapter
        placesAdapter = PlacesAutoCompleteAdapter(this, placesClient)
        editAddress.setAdapter(placesAdapter)
        editAddress.threshold = 3

        // Handle place selection from autocomplete
        editAddress.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as PlacesAutoCompleteAdapter.PlaceAutocomplete
            
            // Fetch full place details
            placesAdapter.getPlaceDetails(selectedItem.placeId) { place ->
                if (place != null) {
                    selectedPlace = place
                    editAddress.setText(place.address ?: selectedItem.toString())
                    editAddress.dismissDropDown()
                    
                    Toast.makeText(
                        this,
                        "Selected: ${place.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Could not get place details",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupSaveButton() {
        buttonSaveLocation.setOnClickListener {
            val address = editAddress.text.toString().trim()

            when {
                address.isEmpty() -> {
                    Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show()
                }
                selectedPlace == null -> {
                    Toast.makeText(this, "Please select an address from suggestions", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Save location logic will be implemented here
                    Toast.makeText(
                        this,
                        "Location saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Clear fields
                    editAddress.text.clear()
                    selectedPlace = null
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up adapter resources
        if (::placesAdapter.isInitialized) {
            placesAdapter.cleanup()
        }
    }
}