package moe.group13.routenode.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R

class RouteNodeAdapter(
    private val items: MutableList<RouteNodeData>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val ITEM_VIEW_TYPE = 0
        const val FOOTER_VIEW_TYPE = 1
    }

    private var suppressSpinnerCallback: Boolean = false

    data class RouteNodeData(
        var no: Int = 1,
        var location: String = "",
        var place: String = "",
        var distance: String = "",
        var additionalRequirements: String = ""
    )

    inner class RouteNodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val spinnerNo: Spinner = itemView.findViewById(R.id.spinnerNo)
        val editLocation: EditText = itemView.findViewById(R.id.editLocation)
        val editPlace: EditText = itemView.findViewById(R.id.editPlace)
        val editDistance: EditText = itemView.findViewById(R.id.editDistance)
        val editAdditional: EditText = itemView.findViewById(R.id.editAdditionalRequirements)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
    }

    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val addButton: View = itemView.findViewById(R.id.buttonAddNode)
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
                addNode()
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

        nodeHolder.editLocation.setText(item.location)
        nodeHolder.editPlace.setText(item.place)
        nodeHolder.editDistance.setText(item.distance)
        nodeHolder.editAdditional.setText(item.additionalRequirements)

        // Show delete button only if there are 2 or more items
        if (items.size >= 2) {
            nodeHolder.buttonDelete.visibility = View.VISIBLE
            nodeHolder.buttonDelete.setOnClickListener {
                deleteNode(position)
            }
        } else {
            nodeHolder.buttonDelete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size) FOOTER_VIEW_TYPE else ITEM_VIEW_TYPE
    }

    fun addNode() {
        items.add(RouteNodeData(no = items.size + 1))
        notifyDataSetChanged()
    }

    private fun deleteNode(position: Int) {
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
    }
}