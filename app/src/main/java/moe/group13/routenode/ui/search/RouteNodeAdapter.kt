package moe.group13.routenode.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.net.PlacesClient
import moe.group13.routenode.R
import moe.group13.routenode.ui.search.ClearButtonHelper
import moe.group13.routenode.ui.search.PlacesAutoCompleteAdapter

class RouteNodeAdapter(
    private val items: MutableList<RouteNodeData>,
    private val placesClient: PlacesClient,
    private val onRetryAi: () -> Unit,
    private val onValidationChanged: ((Boolean) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val ITEM_VIEW_TYPE = 0
        const val FOOTER_VIEW_TYPE = 1
    }

    private var suppressSpinnerCallback: Boolean = false

    private var aiResponse: String? = null
    private var isLoadingAi: Boolean = false

    data class RouteNodeData(
        var no: Int = 1,
        var location: String = "",
        var place: String = "",
        var distance: String = "",
        var additionalRequirements: String = "",
        var hasTriedToSubmit: Boolean = false
    )

    inner class RouteNodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val spinnerNo: Spinner = itemView.findViewById(R.id.spinnerNo)
        val editLocation: AutoCompleteTextView = itemView.findViewById(R.id.editLocation)
        val editPlace: EditText = itemView.findViewById(R.id.editPlace)
        val editDistance: EditText = itemView.findViewById(R.id.editDistance)
        val editAdditional: EditText = itemView.findViewById(R.id.editAdditionalRequirements)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
        val buttonClearAdditional: ImageButton = itemView.findViewById(R.id.buttonClearAdditional)
        val buttonMoreOptions: ImageButton = itemView.findViewById(R.id.buttonMoreOptions)
        val buttonMoreOptionsPlace: ImageButton = itemView.findViewById(R.id.buttonMoreOptionsPlace)
        val errorLocation: TextView = itemView.findViewById(R.id.errorLocation)
        val errorPlace: TextView = itemView.findViewById(R.id.errorPlace)
        val errorDistance: TextView = itemView.findViewById(R.id.errorDistance)
    }

    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val addButton: View = itemView.findViewById(R.id.buttonAddNode)
        val loadingSpinner: View = itemView.findViewById(R.id.progressBarAiLoading)
        val aiChatContainer: View = itemView.findViewById(R.id.aiFooterChatContainer)
        val aiMessage: TextView = itemView.findViewById(R.id.aiFooterMessage)
        val favoriteButton: ImageButton = itemView.findViewById(R.id.buttonFavoriteAi)
        val copyButton: ImageButton = itemView.findViewById(R.id.buttonCopyAi)
        val retryButton: ImageButton = itemView.findViewById(R.id.buttonRetryAi)
    }

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

            footer.addButton.setOnClickListener {
                addNode(footer.itemView.context)
            }

            // Show loading spinner
            footer.loadingSpinner.visibility = if (isLoadingAi) View.VISIBLE else View.GONE

            val hasResponse = !aiResponse.isNullOrBlank()
            footer.aiChatContainer.visibility = if (hasResponse) View.VISIBLE else View.GONE
            if (hasResponse) {
                footer.aiMessage.text = aiResponse
            } else {
                footer.aiMessage.text = ""
            }

            // Favorite icon
            footer.favoriteButton.setOnClickListener {
                // TODO: Implement favorite functionality
            }

            // Copy icon
            footer.copyButton.setOnClickListener {
                val response = aiResponse
                if (!response.isNullOrBlank()) {
                    val context = footer.itemView.context
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("AI response", response)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                }
            }

            // Retry icon
            footer.retryButton.setOnClickListener {
                aiResponse = null
                notifyItemChanged(footer.bindingAdapterPosition)
                onRetryAi()
            }
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
        val autocompleteAdapter = PlacesAutoCompleteAdapter(nodeHolder.itemView.context, placesClient)
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
        
        if (item.hasTriedToSubmit) {
            validateField(nodeHolder.editLocation, nodeHolder.errorLocation, "Location cannot be empty")
            validateField(nodeHolder.editPlace, nodeHolder.errorPlace, "The place you're looking for cannot be empty")
            validateField(nodeHolder.editDistance, nodeHolder.errorDistance, "Distance cannot be empty")
        } else {
            nodeHolder.errorLocation.visibility = View.GONE
            nodeHolder.errorPlace.visibility = View.GONE
            nodeHolder.errorDistance.visibility = View.GONE
        }
        
        nodeHolder.editLocation.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateField(nodeHolder.editLocation, nodeHolder.errorLocation, "Location cannot be empty")
            }
        }
        
        nodeHolder.editPlace.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateField(nodeHolder.editPlace, nodeHolder.errorPlace, "The place you're looking for cannot be empty")
            }
        }
        
        nodeHolder.editDistance.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateField(nodeHolder.editDistance, nodeHolder.errorDistance, "Distance cannot be empty")
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

        removeTextWatcher(nodeHolder.editLocation)
        removeTextWatcher(nodeHolder.editPlace)
        removeTextWatcher(nodeHolder.editDistance)
        removeTextWatcher(nodeHolder.editAdditional)

        addTextWatcher(nodeHolder.editLocation) { text ->
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].location = text
                if (items[pos].hasTriedToSubmit) {
                    validateField(nodeHolder.editLocation, nodeHolder.errorLocation, "Location cannot be empty")
                }
                notifyValidationChanged()
            }
        }
        addTextWatcher(nodeHolder.editPlace) { text ->
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].place = text
                if (items[pos].hasTriedToSubmit) {
                    validateField(nodeHolder.editPlace, nodeHolder.errorPlace, "The place you're looking for cannot be empty")
                }
                notifyValidationChanged()
            }
        }
        addTextWatcher(nodeHolder.editDistance) { text ->
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].distance = text
                if (items[pos].hasTriedToSubmit) {
                    validateField(nodeHolder.editDistance, nodeHolder.errorDistance, "Distance cannot be empty")
                }
                notifyValidationChanged()
            }
        }
        addTextWatcher(nodeHolder.editAdditional) { text ->
            val pos = nodeHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                items[pos].additionalRequirements = text
            }
        }

        // Show delete button only if there are 2 or more items
        if (items.size >= 2) {
            nodeHolder.buttonDelete.visibility = View.VISIBLE
            nodeHolder.buttonDelete.setOnClickListener {
                showDeleteConfirmationDialog(nodeHolder.itemView, position, item.no)
            }
        } else {
            nodeHolder.buttonDelete.visibility = View.GONE
        }

        nodeHolder.buttonMoreOptions.setOnClickListener {
            showMoreOptionsLocationDialog(nodeHolder.itemView)
        }

        nodeHolder.buttonMoreOptionsPlace.setOnClickListener {
            showMoreOptionsPlaceDialog(nodeHolder.itemView)
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

    private fun showDeleteConfirmationDialog(view: View, position: Int, nodeNo: Int) {
        AlertDialog.Builder(view.context)
            .setTitle("Delete Route Node")
            .setMessage("Are you sure you want to delete Route Node No. $nodeNo?")
            .setPositiveButton("Confirm") { dialog, _ ->
                deleteNode(view.context, position, nodeNo)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showMoreOptionsLocationDialog(view: View) {
        AlertDialog.Builder(view.context)
            .setTitle("Use Common Locations")
            .setMessage("Common Locations")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showMoreOptionsPlaceDialog(view: View) {
        AlertDialog.Builder(view.context)
            .setTitle("Use Common Places")
            .setMessage("Common Places")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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

    private fun addTextWatcher(editText: EditText, onTextChanged: (String) -> Unit) {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged(s?.toString() ?: "")
            }
        }
        editText.tag = watcher
        editText.addTextChangedListener(watcher)
    }
    
    private fun addTextWatcher(editText: AutoCompleteTextView, onTextChanged: (String) -> Unit) {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged(s?.toString() ?: "")
            }
        }
        editText.tag = watcher
        editText.addTextChangedListener(watcher)
    }

    private fun removeTextWatcher(editText: EditText) {
        val watcher = editText.tag as? TextWatcher
        if (watcher != null) {
            editText.removeTextChangedListener(watcher)
            editText.tag = null
        }
    }
    
    private fun removeTextWatcher(editText: AutoCompleteTextView) {
        val watcher = editText.tag as? TextWatcher
        if (watcher != null) {
            editText.removeTextChangedListener(watcher)
            editText.tag = null
        }
    }

    private fun validateField(editText: EditText, errorView: TextView, errorMessage: String) {
        val text = editText.text.toString().trim()
        if (text.isEmpty()) {
            errorView.text = errorMessage
            errorView.visibility = View.VISIBLE
        } else {
            errorView.visibility = View.GONE
        }
    }
    
    private fun validateField(editText: AutoCompleteTextView, errorView: TextView, errorMessage: String) {
        val text = editText.text.toString().trim()
        if (text.isEmpty()) {
            errorView.text = errorMessage
            errorView.visibility = View.VISIBLE
        } else {
            errorView.visibility = View.GONE
        }
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