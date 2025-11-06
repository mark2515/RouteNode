package moe.group13.routenode.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
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

    data class RouteNodeData(
        var no: Int = 1,
        var location: String = "",
        var place: String = "",
        var distance: String = "",
        var extraField: String = "",
        var additionalRequirements: String = ""
    )

    inner class RouteNodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val spinnerNo: Spinner = itemView.findViewById(R.id.spinnerNo)
        val editLocation: EditText = itemView.findViewById(R.id.editLocation)
        val editPlace: EditText = itemView.findViewById(R.id.editPlace)
        val editDistance: EditText = itemView.findViewById(R.id.editDistance)
        val editExtraInput: EditText = itemView.findViewById(R.id.editExtraInput)
        val editAdditional: EditText = itemView.findViewById(R.id.editAdditionalRequirements)
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
                val bindingPos = nodeHolder.bindingAdapterPosition
                if (bindingPos != RecyclerView.NO_POSITION) {
                    items[bindingPos].no = pos + 1
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // no-op
            }
        }

        nodeHolder.editLocation.setText(item.location)
        nodeHolder.editPlace.setText(item.place)
        nodeHolder.editDistance.setText(item.distance)
        nodeHolder.editExtraInput.setText(item.extraField)
        nodeHolder.editAdditional.setText(item.additionalRequirements)
    }

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size) FOOTER_VIEW_TYPE else ITEM_VIEW_TYPE
    }

    fun addNode() {
        items.add(RouteNodeData(no = items.size + 1))
        notifyDataSetChanged()
    }
}