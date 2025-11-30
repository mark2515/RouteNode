package moe.group13.routenode.ui.account

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.appbar.MaterialToolbar
import moe.group13.routenode.R

class CommonPlacesActivity : AppCompatActivity() {

    private lateinit var editPlace: EditText
    private lateinit var buttonSavePlace: Button
    private lateinit var tvNoPlaces: TextView
    private lateinit var recyclerPlaces: RecyclerView

    private lateinit var viewModel: SettingsViewModel
    private lateinit var placeAdapter: SavedLocationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_places)

        setupTopAppBar()
        initViews()
        setupViewModel()
        setupSaveButton()
        setupSwipeToDelete()
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

    private fun setupViewModel() {
        viewModel = SettingsViewModel()
        placeAdapter = SavedLocationAdapter(
            onDeleteClick = { item ->
                showDeleteConfirmationDialog(item)
            }
        )

        recyclerPlaces.adapter = placeAdapter
        recyclerPlaces.layoutManager = LinearLayoutManager(this)

        viewModel.loadPlaces()

        viewModel.savedPlaces.observe(this) { list ->
            placeAdapter.submitList(list)
            tvNoPlaces.visibility = if (list.isEmpty()) TextView.VISIBLE else TextView.GONE
        }
    }

    private fun setupSaveButton() {
        buttonSavePlace.setOnClickListener {
            val placeName = editPlace.text.toString().trim()

            if (placeName.isEmpty()) {
                Toast.makeText(this, "Please enter a place name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.savePlace(placeName)
            editPlace.text.clear()

            Toast.makeText(this, "Place saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(item: LocationItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Place")
            .setMessage("Are you sure you want to delete \"${item.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePlace(item.name)
                Toast.makeText(this, "Deleted: ${item.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val item = placeAdapter.getItemAt(vh.adapterPosition)
                viewModel.deletePlace(item.name)

                Toast.makeText(
                    this@CommonPlacesActivity,
                    "Deleted: ${item.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerPlaces)
    }
}
