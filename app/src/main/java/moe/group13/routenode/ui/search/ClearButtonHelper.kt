package moe.group13.routenode.ui.search

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat

object ClearButtonHelper {

    fun setupClearIcon(
        editText: EditText,
        context: Context,
        onClear: () -> Unit
    ) {
        setupClearIconInternal(editText, context, onClear)
    }

    fun setupClearIcon(
        autoCompleteTextView: AutoCompleteTextView,
        context: Context,
        onClear: () -> Unit
    ) {
        setupClearIconInternal(autoCompleteTextView, context, onClear)
    }

    private fun setupClearIconInternal(
        textView: TextView,
        context: Context,
        onClear: () -> Unit
    ) {
        val updateClearIcon = {
            val clearIcon = if (textView.text.isNotEmpty()) {
                ContextCompat.getDrawable(context, android.R.drawable.ic_menu_close_clear_cancel)?.apply {
                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                }
            } else {
                null
            }
            textView.setCompoundDrawables(null, null, clearIcon, null)
            textView.compoundDrawablePadding = 8
        }

        textView.setOnTouchListener { v, event ->
            handleClearIconTouch(v as TextView, event, onClear, updateClearIcon)
        }

        textView.addTextChangedListener(createTextWatcher { updateClearIcon() })

        updateClearIcon()
    }

    private fun handleClearIconTouch(
        textView: TextView,
        event: MotionEvent,
        onClear: () -> Unit,
        updateClearIcon: () -> Unit
    ): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val drawable = textView.compoundDrawables[2]
            if (drawable != null) {
                val clickableArea = drawable.bounds.width() + textView.compoundDrawablePadding
                val touchX = event.x
                val fieldWidth = textView.width

                if (touchX >= fieldWidth - clickableArea - textView.paddingEnd) {
                    textView.setText("")
                    onClear()
                    updateClearIcon()
                    return true
                }
            }
        }
        return false
    }

    fun setupClearButton(
        editText: EditText,
        clearButton: View,
        onClear: () -> Unit
    ) {
        val updateButtonVisibility = {
            clearButton.visibility = if (editText.text.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        clearButton.setOnClickListener {
            editText.setText("")
            onClear()
            updateButtonVisibility()
        }

        editText.addTextChangedListener(createTextWatcher { updateButtonVisibility() })

        updateButtonVisibility()
    }

    private fun createTextWatcher(onTextChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged()
            }
        }
    }
}