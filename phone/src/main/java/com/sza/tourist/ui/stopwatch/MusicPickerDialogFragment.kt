package com.sza.tourist.ui.stopwatch

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.tourist.R
import com.sza.tourist.databinding.DialogMusicPickerBinding
import com.sza.tourist.domain.model.stopwatch.MusicTrackOption
import com.sza.tourist.domain.usecase.stopwatch.LoadMusicTracksUseCase
import com.sza.tourist.help.HelpUi
import com.sza.tourist.ui.dialog.DialogKeyboardDelegate
import com.sza.tourist.ui.stopwatch.helpers.MusicTrackAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The in-app music chooser of the stopwatch settings.
 *
 * Lists MediaStore audio only, and hands the choice to the parent settings dialog through the
 * Fragment Result API - the two fragments hold no references to each other, so the parent's own
 * rotation safety is untouched.
 */
@AndroidEntryPoint
class MusicPickerDialogFragment : DialogFragment() {

    @Inject lateinit var loadMusicTracks: LoadMusicTracksUseCase

    private var _binding: DialogMusicPickerBinding? = null
    private val binding get() = requireNotNull(_binding) { "Binding is only valid while the dialog exists" }

    private lateinit var adapter: MusicTrackAdapter

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        loadTracks()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogMusicPickerBinding.inflate(layoutInflater)
        adapter = MusicTrackAdapter(onTrackClick = ::pick)
        binding.listMusicTracks.adapter = adapter
        binding.listMusicTracks.layoutManager = LinearLayoutManager(requireContext())

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setCustomTitle(HelpUi.dialogTitle(requireContext(), R.string.stopwatch_music_picker_title, "stopwatch.music"))
            .setView(binding.root)
            .create()
        ensurePermissionAndLoadTracks()
        return dialog
    }

    override fun onStart() {
        super.onStart()
        // The track rows are the actions and consume Enter themselves; the delegate adds only the
        // Escape route.
        DialogKeyboardDelegate.applyToDialogFragment(dialog, onConfirm = {})
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun ensurePermissionAndLoadTracks() {
        val permission = requiredAudioPermission()
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            loadTracks()
        } else {
            requestAudioPermission.launch(permission)
        }
    }

    /** On demand and off the frame: the query runs in the repository's IO scope and dies with the dialog. */
    private fun loadTracks() {
        lifecycleScope.launch {
            val tracks = loadMusicTracks()
            val currentBinding = _binding ?: return@launch
            adapter.submitList(tracks)
            currentBinding.textMusicPickerEmpty.isVisible = tracks.isEmpty()
        }
    }

    private fun pick(track: MusicTrackOption) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(
                KEY_URI to track.uri.toString(),
                KEY_TITLE to track.title,
            ),
        )
        dismiss()
    }

    companion object {
        const val TAG = "MusicPickerDialog"
        const val REQUEST_KEY = "stopwatch_music_pick"
        const val KEY_URI = "picked_uri"
        const val KEY_TITLE = "picked_title"

        private fun requiredAudioPermission(): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        fun newInstance(): MusicPickerDialogFragment = MusicPickerDialogFragment()
    }
}
