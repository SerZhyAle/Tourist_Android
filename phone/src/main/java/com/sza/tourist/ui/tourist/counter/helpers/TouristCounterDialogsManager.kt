package com.sza.tourist.ui.tourist.counter.helpers

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.tourist.R
import com.sza.tourist.databinding.DialogTouristCounterStartBinding
import com.sza.tourist.domain.model.tourist.TouristCounterState
import com.sza.tourist.help.HelpUi
import com.sza.tourist.util.showBoundToHost

/** The three confirmations of the counter screen - starting number, reset and clearing history. */
class TouristCounterDialogsManager(
    private val context: Context,
) {

    fun confirmReset(startValue: Long, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.tourist_counter_reset_title)
            .setMessage(context.getString(R.string.tourist_counter_reset_message, startValue.toString()))
            .setPositiveButton(R.string.tourist_counter_reset) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel, null)
            .showBoundToHost(context)
            ?.let { com.sza.tourist.ui.common.widget.DialogCommandPolicy.apply(it, destructive = true) }
    }

    fun confirmClearHistory(onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.tourist_counter_clear_history_title)
            .setPositiveButton(R.string.tourist_counter_clear_history) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel, null)
            .showBoundToHost(context)
            ?.let { com.sza.tourist.ui.common.widget.DialogCommandPolicy.apply(it, destructive = true) }
    }

    /** A blank or out-of-range entry keeps the dialog open with an error rather than silently doing nothing. */
    fun askStartValue(current: Long, onEntered: (Long) -> Unit) {
        val binding = DialogTouristCounterStartBinding.inflate(LayoutInflater.from(context))
        binding.editStartValue.setText(current.toString())
        binding.editStartValue.selectAll()
        val dialog = MaterialAlertDialogBuilder(context)
            .setCustomTitle(HelpUi.dialogTitle(context, R.string.tourist_counter_set_start, "counter.start"))
            .setView(binding.root)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        val submit = {
            val entered = binding.editStartValue.text?.toString()?.trim()?.toLongOrNull()
            if (entered == null || entered !in 0L..TouristCounterState.MAX_VALUE) {
                binding.layoutStartValue.error = context.getString(R.string.tourist_counter_start_invalid)
            } else {
                dialog.dismiss()
                onEntered(entered)
            }
        }
        binding.editStartValue.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) submit()
            actionId == EditorInfo.IME_ACTION_DONE
        }
        HelpUi.bindDialogKey(dialog, context, "counter.start")
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { submit() }
            com.sza.tourist.ui.common.widget.DialogCommandPolicy.apply(dialog)
        }
        dialog.showBoundToHost(context)
    }
}
