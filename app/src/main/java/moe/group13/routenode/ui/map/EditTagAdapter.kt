import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.radiobutton.MaterialRadioButton
import moe.group13.routenode.R


class EditTagAdapter(
    private val tags: List<String>
) : RecyclerView.Adapter<EditTagAdapter.TagViewHolder>() {

    private var selectedTag: String? = null
    private var selectedPosition: Int = -1

    class TagViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textOption: TextView = view.findViewById(R.id.textOption)
        val radioButton: MaterialRadioButton = view.findViewById(R.id.tag_radio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_swap_option, parent, false)
        return TagViewHolder(view)
    }

    override fun getItemCount(): Int = tags.size

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]
        holder.textOption.text = tag

        holder.radioButton.isChecked = tag == selectedTag

        //click on row selects radio
        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            selectedTag = tag
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
        }
        // click the radio itself
        holder.radioButton.setOnClickListener {
            holder.itemView.performClick()
        }

    }

    fun getSelectedTag(): String? = selectedTag
}
