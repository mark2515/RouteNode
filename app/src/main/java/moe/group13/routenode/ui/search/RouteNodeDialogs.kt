package moe.group13.routenode.ui.search

import android.content.Context
import androidx.appcompat.app.AlertDialog

object RouteNodeDialogs {
    
    fun showDeleteConfirmation(
        context: Context,
        nodeNo: Int,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Delete Route Node")
            .setMessage("Are you sure you want to delete Route Node No. $nodeNo?")
            .setPositiveButton("Confirm") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    fun showMoreOptionsLocation(context: Context) {
        AlertDialog.Builder(context)
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
    
    fun showMoreOptionsPlace(context: Context) {
        AlertDialog.Builder(context)
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
}