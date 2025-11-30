package moe.group13.routenode.ui.account

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
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

    private lateinit var viewModel: SettingsViewModel
    private lateinit var locationAdapter: SavedLocationAdapter

    private var selectedPlace: Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_locations)

        setupTopAppBar()
        initializePlaces()
        initViews()
        setupAddressSearch()
        setupViewModel()
        setupSaveButton()
        setupSwipeToDelete()   // <-- Add swipe behavior
    }

    private fun setupTopAppBar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initializePlaces() {
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

    private fun setupViewModel() {
        viewModel = SettingsViewModel()
        locationAdapter = SavedLocationAdapter()

        recyclerLocations.adapter = locationAdapter
        recyclerLocations.layoutManager = LinearLayoutManager(this)

        viewModel.loadLocations()

        viewModel.savedLocations.observe(this) { list ->
            locationAdapter.submitList(list)
            tvNoLocations.visibility = if (list.isEmpty()) TextView.VISIBLE else TextView.GONE
        }
    }

    private fun setupAddressSearch() {
        placesAdapter = PlacesAutoCompleteAdapter(this, placesClient)
        editAddress.setAdapter(placesAdapter)
        editAddress.threshold = 3

        editAddress.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem =
                parent.getItemAtPosition(position) as PlacesAutoCompleteAdapter.PlaceAutocomplete

            placesAdapter.getPlaceDetails(selectedItem.placeId) { place ->
                if (place != null) {
                    selectedPlace = place
                    editAddress.setText(place.address ?: selectedItem.toString())
                    editAddress.dismissDropDown()
                    Toast.makeText(this, "Selected: ${place.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Could not get place details", Toast.LENGTH_SHORT).show()
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
                    showNameDialog(address)
                    editAddress.text.clear()
                    selectedPlace = null
                }
            }
        }
    }

    private fun showNameDialog(address: String) {
        val editText = EditText(this)
        editText.hint = "Enter a name"

        AlertDialog.Builder(this)
            .setTitle("Name This Location")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotBlank()) {
                    viewModel.saveLocation(address, name)
                } else {
                    Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = locationAdapter.getItemAt(position)

                viewModel.deleteLocation(item.name, item.address)

                Toast.makeText(
                    this@CommonLocationsActivity,
                    "Deleted: ${item.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerLocations)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::placesAdapter.isInitialized) {
            placesAdapter.cleanup()
        }
    }
}
