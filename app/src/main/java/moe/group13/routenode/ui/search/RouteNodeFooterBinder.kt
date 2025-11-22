package moe.group13.routenode.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Toast
import moe.group13.routenode.R
import moe.group13.routenode.utils.MarkdownRenderer

object RouteNodeFooterBinder {
    
    fun bind(
        holder: FooterViewHolder,
        aiResponse: String?,
        isLoadingAi: Boolean,
        onAddNode: () -> Unit,
        onRetryAi: () -> Unit,
        onFavoriteAi: () -> Unit = {}
    ) {
        // Setup add button
        holder.addButton.setOnClickListener {
            onAddNode()
        }
        
        // Show loading spinner
        holder.loadingSpinner.visibility = if (isLoadingAi) View.VISIBLE else View.GONE
        
        // Show AI response container
        val hasResponse = !aiResponse.isNullOrBlank()
        holder.aiChatContainer.visibility = if (hasResponse) View.VISIBLE else View.GONE
        
        if (hasResponse) {
            // Render markdown formatted text
            MarkdownRenderer.render(holder.aiMessage, aiResponse)
        } else {
            holder.aiMessage.text = ""
        }
        
        // Favorite button
        holder.favoriteButton.setOnClickListener {
            onFavoriteAi()
        }
        
        // Copy button
        holder.copyButton.setOnClickListener {
            copyAiResponse(holder.itemView.context, aiResponse)
        }
        
        // Retry button
        holder.retryButton.setOnClickListener {
            onRetryAi()
        }
    }
    
    private fun copyAiResponse(context: Context, response: String?) {
        if (!response.isNullOrBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // Copy the plain text version
            val plainText = MarkdownRenderer.cleanMarkdown(response)
            val clip = ClipData.newPlainText("AI response", plainText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                context,
                context.getString(R.string.copied_to_clipboard),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}