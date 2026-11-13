package com.sza.tourist.ui.dialog

import android.app.Dialog
import android.view.KeyEvent
import android.widget.AdapterView
import android.widget.EditText

/**
 * Wires standard keyboard contract onto any [Dialog]:
 *  - Enter  -> [onConfirm], unless the focused view owns Enter itself (see [focusedViewOwnsEnter])
 *  - Escape -> [Dialog.dismiss]
 *
 * The shortcut-sheet and F1 help dispatch of FastMediaSorter stay behind with the in-app help
 * (S0014).
 */
object DialogKeyboardDelegate {

    fun applyTo(dialog: Dialog, onConfirm: () -> Unit) {
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_UP) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    if (focusedViewOwnsEnter(dialog)) {
                        false
                    } else {
                        onConfirm()
                        true
                    }
                }
                KeyEvent.KEYCODE_ESCAPE -> {
                    dialog.dismiss()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Variant for [androidx.fragment.app.DialogFragment]: wires the contract onto the
     * underlying [Dialog] via [android.content.DialogInterface.OnKeyListener] after the dialog is shown.
     * Call from [androidx.fragment.app.DialogFragment.onStart].
     */
    fun applyToDialogFragment(dialog: Dialog?, onConfirm: () -> Unit) {
        dialog ?: return
        applyTo(dialog, onConfirm)
    }

    /**
     * Dialog.dispatchKeyEvent runs this listener before the window, so consuming Enter here would
     * steal the ACTION_UP that View.onKeyUp turns into a click: Enter on a focused Cancel button
     * would run the confirm action, Enter on a list row would do nothing. A focused text field is
     * the exception - Enter there is the keyboard "accept" and still confirms the dialog.
     */
    private fun focusedViewOwnsEnter(dialog: Dialog): Boolean {
        val focused = dialog.currentFocus ?: return false
        return focused !is EditText && (focused.isClickable || focused is AdapterView<*>)
    }
}
