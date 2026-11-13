package com.sza.tourist.ui.common.widget

import android.content.DialogInterface
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.material.color.MaterialColors

/** Product-owned answer bars retain every answer in reading order and default to dismissal. */
object DialogCommandPolicy {
    fun apply(dialog: AlertDialog, destructive: Boolean = false) {
        val cancel = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        val apply = dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
        val confirm = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        val answers = listOfNotNull(cancel, apply, confirm).filter { it.visibility == android.view.View.VISIBLE }
        val old = answers.firstOrNull()?.parent as? ViewGroup ?: return
        val flow = CommandFlowLayout(dialog.context)
        answers.forEach { answer ->
            (answer.parent as? ViewGroup)?.removeView(answer)
            answer.isAllCaps = false
            answer.maxLines = Int.MAX_VALUE
            answer.ellipsize = null
            flow.addView(answer, ViewGroup.MarginLayoutParams(-2, -2))
        }
        old.removeAllViews()
        old.addView(flow, ViewGroup.LayoutParams(-1, -2))
        if (destructive && confirm != null) {
            confirm.backgroundTintList = android.content.res.ColorStateList.valueOf(
                MaterialColors.getColor(confirm, androidx.appcompat.R.attr.colorError))
            confirm.setTextColor(MaterialColors.getColor(confirm, com.google.android.material.R.attr.colorOnError))
        }
        cancel?.isFocusableInTouchMode = true
        cancel?.requestFocus()
        if (destructive) dialog.setOnKeyListener { _, key, event ->
            when {
                key == KeyEvent.KEYCODE_ESCAPE -> { if (event.action == KeyEvent.ACTION_UP) dialog.dismiss(); true }
                (key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_NUMPAD_ENTER) && dialog.currentFocus !in answers -> {
                    if (event.action == KeyEvent.ACTION_UP) dialog.dismiss()
                    true
                }
                else -> false
            }
        }
    }
}
