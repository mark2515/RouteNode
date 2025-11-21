package moe.group13.routenode.ui.search

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView

object RouteNodeValidationHelper {
    
    fun addTextWatcher(editText: EditText, onTextChanged: (String) -> Unit) {
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
    
    fun addTextWatcher(editText: AutoCompleteTextView, onTextChanged: (String) -> Unit) {
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
    
    fun removeTextWatcher(editText: EditText) {
        val watcher = editText.tag as? TextWatcher
        if (watcher != null) {
            editText.removeTextChangedListener(watcher)
            editText.tag = null
        }
    }
    
    fun removeTextWatcher(editText: AutoCompleteTextView) {
        val watcher = editText.tag as? TextWatcher
        if (watcher != null) {
            editText.removeTextChangedListener(watcher)
            editText.tag = null
        }
    }
    
    fun validateField(editText: EditText, errorView: TextView, errorMessage: String) {
        val text = editText.text.toString().trim()
        if (text.isEmpty()) {
            errorView.text = errorMessage
            errorView.visibility = View.VISIBLE
        } else {
            errorView.visibility = View.GONE
        }
    }
    
    fun validateField(editText: AutoCompleteTextView, errorView: TextView, errorMessage: String) {
        val text = editText.text.toString().trim()
        if (text.isEmpty()) {
            errorView.text = errorMessage
            errorView.visibility = View.VISIBLE
        } else {
            errorView.visibility = View.GONE
        }
    }
}