package com.sza.tourist.ui.stopwatch

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.tourist.R
import com.sza.tourist.core.capability.CapabilityAvailability
import com.sza.tourist.databinding.DialogStopwatchSettingsBinding
import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.repository.MusicTrackRepository
import com.sza.tourist.domain.repository.SettingsRepository
import com.sza.tourist.help.HelpUi
import com.sza.tourist.ui.dialog.DialogKeyboardDelegate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * The stopwatch's own settings window.
 *
 * Takes its state from the shared settings repository rather than from a host screen's ViewModel, and
 * the track row opens the in-app music picker.
 */
@AndroidEntryPoint
class StopwatchSettingsDialogFragment : DialogFragment() {

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var mediaCapabilities: CapabilityAvailability

    @Inject lateinit var musicTrackRepository: MusicTrackRepository

    private var _binding: DialogStopwatchSettingsBinding? = null
    private val binding get() = requireNotNull(_binding) { "Binding is only valid while the dialog exists" }

    /** Edited in place and written back only when the user confirms, so cancel really cancels. */
    private var draft: AppSettings? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogStopwatchSettingsBinding.inflate(layoutInflater)
        bindStaticRows()
        childFragmentManager.setFragmentResultListener(MusicPickerDialogFragment.REQUEST_KEY, this) { _, bundle ->
            val uri = bundle.getString(MusicPickerDialogFragment.KEY_URI)
            if (!uri.isNullOrBlank()) {
                onTrackChosen(uri.toUri(), bundle.getString(MusicPickerDialogFragment.KEY_TITLE).orEmpty())
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setCustomTitle(HelpUi.dialogTitle(requireContext(), R.string.stopwatch_settings_title, "stopwatch.settings"))
            .setView(binding.root)
            .create()

        binding.btnStopwatchSettingsCancel.setOnClickListener { dialog.dismiss() }
        binding.btnStopwatchSettingsApply.setOnClickListener {
            applyAndDismiss(dialog)
        }
        loadDraft(savedInstanceState)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        DialogKeyboardDelegate.applyToDialogFragment(dialog) {
            binding.btnStopwatchSettingsApply.performClick()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val edited = draft ?: return
        outState.putInt(KEY_PARTICIPANT_COUNT, edited.stopwatchParticipantCount)
        outState.putBoolean(KEY_MUSIC_ENABLED, edited.stopwatchMusicEnabled)
        outState.putString(KEY_MUSIC_URI, edited.stopwatchMusicUri)
        outState.putBoolean(KEY_VOLUME_KEYS, edited.stopwatchVolumeKeysControl)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindStaticRows() {
        binding.rowStopwatchParticipants.setEntries(
            AppSettings.STOPWATCH_PARTICIPANT_OPTIONS.map { count -> count.toString() },
        )
        val audio = mediaCapabilities.supportsAudio
        binding.rowStopwatchMusic.isVisible = audio
        binding.rowStopwatchTrack.isVisible = audio
        binding.rowStopwatchTrack.setOnRowClickListener { openTrackPicker() }
    }

    /**
     * The rows are filled from the stored settings once the first snapshot arrives.
     *
     * After a rotation the edits saved in [onSaveInstanceState] are laid over the fresh snapshot, so the
     * rows the dialog restored and a track picked before the rotation are not replaced by stored values.
     */
    private fun loadDraft(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            val settings = settingsRepository.getSettings().first().withSavedEdits(savedInstanceState)
            draft = settings
            renderDraft(settings)
            bindListeners()
        }
    }

    private suspend fun trackLabelFor(uri: String): CharSequence {
        if (uri.isEmpty()) {
            return getString(R.string.stopwatch_settings_track_none)
        }
        val title = musicTrackRepository.titleOf(uri.toUri())
        return title ?: uri.toUri().lastPathSegment ?: uri
    }

    private fun renderDraft(settings: AppSettings) {
        val index = AppSettings.STOPWATCH_PARTICIPANT_OPTIONS
            .indexOf(settings.stopwatchParticipantCount)
            .coerceAtLeast(0)
        binding.rowStopwatchParticipants.setSelection(index)
        binding.rowStopwatchMusic.setCheckedSilently(settings.stopwatchMusicEnabled)
        binding.rowStopwatchVolumeKeys.setCheckedSilently(settings.stopwatchVolumeKeysControl)
        lifecycleScope.launch {
            val label = trackLabelFor(settings.stopwatchMusicUri)
            _binding?.rowStopwatchTrack?.setValue(label)
        }
    }

    private fun bindListeners() {
        binding.rowStopwatchParticipants.setOnItemSelectedListener { index ->
            val count = AppSettings.STOPWATCH_PARTICIPANT_OPTIONS.getOrNull(index) ?: return@setOnItemSelectedListener
            draft = draft?.copy(stopwatchParticipantCount = count)
        }
        binding.rowStopwatchMusic.setOnCheckedChangeListener { checked ->
            draft = draft?.copy(stopwatchMusicEnabled = checked)
        }
        binding.rowStopwatchVolumeKeys.setOnCheckedChangeListener { checked ->
            draft = draft?.copy(stopwatchVolumeKeysControl = checked)
        }
    }

    private fun openTrackPicker() {
        runCatching {
            MusicPickerDialogFragment.newInstance()
                .show(childFragmentManager, MusicPickerDialogFragment.TAG)
        }.onFailure { error ->
            Timber.w(error, "Stopwatch settings could not open the music picker")
        }
    }

    private fun onTrackChosen(uri: Uri, title: String) {
        draft = draft?.copy(stopwatchMusicUri = uri.toString(), stopwatchMusicEnabled = true)
        binding.rowStopwatchTrack.setValue(
            title.ifBlank { uri.lastPathSegment ?: uri.toString() },
        )
        binding.rowStopwatchMusic.setCheckedSilently(true)
    }

    private fun applyAndDismiss(dialog: Dialog) {
        val edited = draft
        if (edited == null) {
            dialog.dismiss()
            return
        }
        lifecycleScope.launch {
            settingsRepository.updateSettings { latest ->
                latest.copy(
                    stopwatchParticipantCount = edited.stopwatchParticipantCount,
                    stopwatchMusicEnabled = edited.stopwatchMusicEnabled,
                    stopwatchMusicUri = edited.stopwatchMusicUri,
                    stopwatchVolumeKeysControl = edited.stopwatchVolumeKeysControl,
                )
            }
            dialog.dismiss()
        }
    }

    companion object {
        const val TAG = "StopwatchSettingsDialog"

        private const val KEY_PARTICIPANT_COUNT = "stopwatch_settings_participant_count"
        private const val KEY_MUSIC_ENABLED = "stopwatch_settings_music_enabled"
        private const val KEY_MUSIC_URI = "stopwatch_settings_music_uri"
        private const val KEY_VOLUME_KEYS = "stopwatch_settings_volume_keys"

        private fun AppSettings.withSavedEdits(saved: Bundle?): AppSettings {
            if (saved == null || !saved.containsKey(KEY_PARTICIPANT_COUNT)) return this
            return copy(
                stopwatchParticipantCount = saved.getInt(KEY_PARTICIPANT_COUNT, stopwatchParticipantCount),
                stopwatchMusicEnabled = saved.getBoolean(KEY_MUSIC_ENABLED, stopwatchMusicEnabled),
                stopwatchMusicUri = saved.getString(KEY_MUSIC_URI) ?: stopwatchMusicUri,
                stopwatchVolumeKeysControl = saved.getBoolean(KEY_VOLUME_KEYS, stopwatchVolumeKeysControl),
            )
        }

        fun newInstance(): StopwatchSettingsDialogFragment = StopwatchSettingsDialogFragment()
    }
}
