package moe.group13.routenode.ui.search

import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R

class RouteNodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
    val textKm: TextView = itemView.findViewById(R.id.textKm)
}

class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val addButton: View = itemView.findViewById(R.id.buttonAddNode)
    val loadingSpinner: View = itemView.findViewById(R.id.progressBarAiLoading)
    val aiChatContainer: View = itemView.findViewById(R.id.aiFooterChatContainer)
    val aiMessage: TextView = itemView.findViewById(R.id.aiFooterMessage)
    val favoriteButton: ImageButton = itemView.findViewById(R.id.buttonFavoriteAi)
    val copyButton: ImageButton = itemView.findViewById(R.id.buttonCopyAi)
    val retryButton: ImageButton = itemView.findViewById(R.id.buttonRetryAi)
}