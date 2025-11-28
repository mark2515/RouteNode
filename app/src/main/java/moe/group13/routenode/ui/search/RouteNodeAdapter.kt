package moe.group13.routenode.ui.search

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.net.PlacesClient
import moe.group13.routenode.R

class RouteNodeAdapter(
    private val items: MutableList<RouteNodeData>,
    private val placesClient: PlacesClient,
    private val onRetryAi: () -> Unit,
    private val onValidationChanged: ((Boolean) -> Unit)? = null,
    private val onFavoriteAi: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val ITEM_VIEW_TYPE = 0
        const val FOOTER_VIEW_TYPE = 1
    }

    private var suppressSpinnerCallback: Boolean = false

    private var aiResponse: String? = null
    private var isLoadingAi: Boolean = false
    
    // Cache autocomplete adapters to prevent memory leaks and repeated creation
    private val autocompleteAdapters = mutableMapOf<Int, PlacesAutoCompleteAdapter>()

    data class RouteNodeData(
        var no: Int = 1,
        var location: String = "",
        var place: String = "",
        var distance: String = "",
        var additionalRequirements: String = "",
        var hasTriedToSubmit: Boolean = false
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == FOOTER_VIEW_TYPE) {
            val view = inflater.inflate(R.layout.item_add_node_footer, parent, false)
            FooterViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_route_node, parent, false)
            RouteNodeViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == FOOTER_VIEW_TYPE) {
            val footer = holder as FooterViewHolder
            
            RouteNodeFooterBinder.bind(
                holder = footer,
                aiResponse = aiResponse,
                isLoadingAi = isLoadingAi,
                onAddNode = { addNode(footer.itemView.context) },
                onRetryAi = {
                    aiResponse = null
                    notifyItemChanged(footer.bindingAdapterPosition)
                    onRetryAi()
                },
                onFavoriteAi = {
                    onFavoriteAi?.invoke()
                }
            )
            return
        }

        val nodeHolder = holder as RouteNodeViewHolder
        val item = items[position]

        // Update spinner choices
        val count = items.size
        val numbers = (1..count).map { it.toString() }
        val context = nodeHolder.itemView.context
        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, numbers)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        nodeHolder.spinnerNo.adapter = spinnerAdapter

        val currentNo = item.no.coerceIn(1, count)
        nodeHolder.spinnerNo.setSelection(currentNo - 1, false)

        nodeHolder.spinnerNo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (suppressSpinnerCallback) return

                val bindingPos = nodeHolder.bindingAdapterPosition
                if (bindingPos == RecyclerView.NO_POSITION) return

                val newNo = pos + 1
                val currentItem = items[bindingPos]
                val oldNo = currentItem.no

                if (newNo == oldNo) return

                val otherIndex = items.indexOfFirst { it.no == newNo && it !== currentItem }

                suppressSpinnerCallback = true
                if (otherIndex >= 0) {
                    items[bindingPos].no = newNo
                    items[otherIndex].no = oldNo
                    notifyItemChanged(bindingPos)
                    notifyItemChanged(otherIndex)
                } else {
                    items[bindingPos].no = newNo
                    notifyItemChanged(bindingPos)
                }
                suppressSpinnerCallback = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // no-op
            }
        }

        // Setup Google Places Autocomplete for location field
        // Reuse adapter if already created for this position
        val autocompleteAdapter = autocompleteAdapters.getOrPut(position) {
            PlacesAutoCompleteAdapter(nodeHolder.itemView.context, placesClient)
        }
        nodeHolder.editLocation.setAdapter(autocompleteAdapter)
        nodeHolder.editLocation.threshold = 3
        
        // Handle place selection from autocomplete
        nodeHolder.editLocation.setOnItemClickListener { parent, _, position, _ ->
            val selectedPlace = autocompleteAdapter.getItem(position)
            if (selectedPlace != null) {
                val pos = nodeHolder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                    // Update the location with the full address
                    items[pos].location = selectedPlace.toString()
                    nodeHolder.editLocation.setText(selectedPlace.toString())
                    
                    Log.d("RouteNodeAdapter", "Selected place: ${selectedPlace.primaryText}, ${selectedPlace.secondaryText}")
                }
            }
        }
        
        nodeHolder.editLocation.setText(item.location)
        nodeHolder.editPlace.setText(item.place)
        nodeHolder.editDistance.setText(item.distance)
        nodeHolder.editAdditional.setText(item.additionalRequirements)
        
        // Update distance unit label based on settings
        val prefs = context.getSharedPreferences("route_settings", Context.MODE_PRIVATE)
        val unitIndex = prefs.getInt("unit_index", 0)
        val unitLabel = if (unitIndex == 1) "mi" else "km"
        nodeHolder.textKm.text = unitLabel
        
        if (item.hasTriedToSubmit) {
            RouteNodeValidationHelper.validateField(nodeHolder.editLocation, nodeHolder.errorLocation, "Location cannot be empty")
            RouteNodeValidationHelper.validateField(nodeHolder.editPlace, nodeHolder.errorPlace, "The place you're looking for cannot be empty")
            RouteNodeValidationHelper.validateField(nodeHolder.editDistance, nodeHolder.errorDistance, "Distance cannot be empty")
        } else {
            nodeHolder.errorLocation.visibility = View.GONE
            nodeHolder.errorPlace.visibility = View.GONE
            nodeHolder.errorDistance.visibility = View.GONE
        }
        
        nodeHolder.editLocation.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                RouteNodeValidationHelper.validateField(nodeHolder.editLocation, nodeHolder.errorLocation, "Location cannot be empty")
            }
        }
        
        nodeHolder.editPlace.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                RouteNodeValidationHelper.validateField(nodeHolder.editPlace, nodeHolder.errorPlace, "The place you're looking for cannot be empty")
            }
        }
        
        nodeHolder.editDistance.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Apply ceiling to the distance value
                val currentText = nodeHolder.editDistance.text.toString()
                if (currentText.isNotEmpty()) {
                    try {
                        val value = currentText.toDouble()
                        if (value > 0) {
                            val ceiledValue = kotlin.math.ceil(value).toInt()
                            nodeHolder.editDistance.setText(ceiledValue.toString())
                            val pos = nodeHolder.bindingAdapterPosition
                            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                                items[pos].distance = ceiledValue.toString()
                            }
                        }
                    } catch (e: NumberFormatException) {
                    }
                }
                RouteNodeValidationHelper.validateField(nodeHolder.editDistance, nodeHolder.errorDistance, "Distance cannot be empty")
            }
        }
        
        // Setup clear icons using helper
        ClearButtonHelper.setupClearIcon(
            nodeHolder.editLocation,
            nodeHolder.itemView.context
        ) {
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].location = ""
            }
        }
        
        ClearButtonHelper.setupClearIcon(
            nodeHolder.editPlace,
            nodeHolder.itemView.context
        ) {
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].place = ""
            }
        }
        
        ClearButtonHelper.setupClearIcon(
            nodeHolder.editDistance,
            nodeHolder.itemView.context
        ) {
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].distance = ""
            }
        }
        
        ClearButtonHelper.setupClearButton(
            nodeHolder.editAdditional,
            nodeHolder.buttonClearAdditional
        ) {
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].additionalRequirements = ""
            }
        }

        RouteNodeValidationHelper.removeTextWatcher(nodeHolder.editLocation)
        RouteNodeValidationHelper.removeTextWatcher(nodeHolder.editPlace)
        RouteNodeValidationHelper.removeTextWatcher(nodeHolder.editDistance)
        RouteNodeValidationHelper.removeTextWatcher(nodeHolder.editAdditional)

        RouteNodeValidationHelper.addTextWatcher(nodeHolder.editLocation) { text ->
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].location = text
                if (items[pos].hasTriedToSubmit) {
                    RouteNodeValidationHelper.validateField(nodeHolder.editLocation, nodeHolder.errorLocation, "Location cannot be empty")
                }
                notifyValidationChanged()
            }
        }
        RouteNodeValidationHelper.addTextWatcher(nodeHolder.editPlace) { text ->
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].place = text
                if (items[pos].hasTriedToSubmit) {
                    RouteNodeValidationHelper.validateField(nodeHolder.editPlace, nodeHolder.errorPlace, "The place you're looking for cannot be empty")
                }
                notifyValidationChanged()
            }
        }
        RouteNodeValidationHelper.addTextWatcher(nodeHolder.editDistance) { text ->
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].distance = text
                if (items[pos].hasTriedToSubmit) {
                    RouteNodeValidationHelper.validateField(nodeHolder.editDistance, nodeHolder.errorDistance, "Distance cannot be empty")
                }
                notifyValidationChanged()
            }
        }
        RouteNodeValidationHelper.addTextWatcher(nodeHolder.editAdditional) { text ->
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].additionalRequirements = text
            }
        }

        // Show delete button only if there are 2 or more items
        if (items.size >= 2) {
            nodeHolder.buttonDelete.visibility = View.VISIBLE
            nodeHolder.buttonDelete.setOnClickListener {
                RouteNodeDialogs.showDeleteConfirmation(
                    nodeHolder.itemView.context,
                    item.no
                ) {
                    deleteNode(nodeHolder.itemView.context, position, item.no)
                }
            }
        } else {
            nodeHolder.buttonDelete.visibility = View.GONE
        }

        nodeHolder.buttonMoreOptions.setOnClickListener {
            RouteNodeDialogs.showMoreOptionsLocation(nodeHolder.itemView.context)
        }

        nodeHolder.buttonMoreOptionsPlace.setOnClickListener {
            RouteNodeDialogs.showMoreOptionsPlace(nodeHolder.itemView.context)
        }
    }

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size) FOOTER_VIEW_TYPE else ITEM_VIEW_TYPE
    }

    fun setAiResponse(response: String?) {
        aiResponse = response
        notifyItemChanged(items.size)
    }

    fun setLoadingState(isLoading: Boolean) {
        isLoadingAi = isLoading
        notifyItemChanged(items.size)
    }

    fun addNode(context: Context) {
        if (items.size >= 5) {
            Toast.makeText(context, "Maximum 5 Route Nodes allowed", Toast.LENGTH_SHORT).show()
            return
        }
        items.add(RouteNodeData(no = items.size + 1))
        notifyDataSetChanged()
        notifyValidationChanged()
    }

    private fun deleteNode(context: android.content.Context, position: Int, nodeNo: Int) {
        if (position < 0 || position >= items.size || items.size < 2) {
            return
        }

        val deletedNo = items[position].no
        
        // Remove the item
        items.removeAt(position)

        items.forEach { item ->
            if (item.no > deletedNo) {
                item.no -= 1
            }
        }

        // Notify the adapter of the change
        notifyDataSetChanged()

        // Show toast message
        Toast.makeText(context, "Route Node No. $nodeNo Deleted", Toast.LENGTH_SHORT).show()
        
        // Trigger validation after deletion
        notifyValidationChanged()
    }

    fun isAllFieldsValid(): Boolean {
        return items.all { 
            it.location.trim().isNotEmpty() && 
            it.place.trim().isNotEmpty() && 
            it.distance.trim().isNotEmpty()
        }
    }

    private fun notifyValidationChanged() {
        onValidationChanged?.invoke(isAllFieldsValid())
    }
    
    fun validateAllFields() {
        notifyValidationChanged()
    }
    
    fun showAllValidationErrors() {
        items.forEach { it.hasTriedToSubmit = true }
        notifyDataSetChanged()
    }
    
    fun getRouteNodeData(): List<RouteNodeData> {
        return items.toList()
    }
    
    fun setRouteNodeData(newData: List<RouteNodeData>) {
        items.clear()
        items.addAll(newData.mapIndexed { index, data -> 
            data.copy(no = index + 1)
        })
        notifyDataSetChanged()
    }
    
    fun cleanup() {
        autocompleteAdapters.values.forEach { it.cleanup() }
        autocompleteAdapters.clear()
    }
    
    fun updateDistanceUnits() {
        notifyItemRangeChanged(0, items.size)
    }
    
    fun reset() {
        items.clear()
        items.add(RouteNodeData(no = 1))
        aiResponse = null
        notifyDataSetChanged()
        notifyValidationChanged()
    }
}