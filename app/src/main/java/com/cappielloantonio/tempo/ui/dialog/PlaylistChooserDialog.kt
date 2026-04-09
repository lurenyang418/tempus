package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogPlaylistChooserBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.adapter.PlaylistDialogHorizontalAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PlaylistChooserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlaylistChooserDialog : DialogFragment(), ClickCallback {
    private var bind: DialogPlaylistChooserBinding? = null
    private var playlistChooserViewModel: PlaylistChooserViewModel? = null
    private var playlistDialogHorizontalAdapter: PlaylistDialogHorizontalAdapter? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        DialogPlaylistChooserBinding.inflate(getLayoutInflater())
        bind = DialogPlaylistChooserBinding.inflate(getLayoutInflater())

        playlistChooserViewModel =
            ViewModelProvider(requireActivity()).get<PlaylistChooserViewModel>(
                PlaylistChooserViewModel::class.java
            )

        bind!!.playlistDialogChooserVisibilitySwitch.setOnCheckedChangeListener(
            CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                playlistChooserViewModel!!.setIsPlaylistPublic(
                    isChecked
                )
            }
        )
        bind!!.playlistChooserDialogCreateButton.setOnClickListener(View.OnClickListener { v: View? -> launchPlaylistEditor() })
        bind!!.playlistChooserDialogCancelButton.setOnClickListener(View.OnClickListener { v: View? -> dismiss() })

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(bind!!.getRoot())
            .setTitle(R.string.playlist_chooser_dialog_title)
        return builder.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    override fun onStart() {
        super.onStart()

        initPlaylistView()
        setSongInfo()
    }

    private fun setSongInfo() {
        playlistChooserViewModel!!.songsToAdd =
            requireArguments().getParcelableArrayList<Child?>(Constants.TRACKS_OBJECT)!!
    }

    private fun launchPlaylistEditor() {
        val bundle = Bundle()
        bundle.putParcelableArrayList(
            Constants.TRACKS_OBJECT,
            playlistChooserViewModel!!.songsToAdd
        )

        val editorDialog = PlaylistEditorDialog(null)
        editorDialog.setArguments(bundle)
        editorDialog.show(
            requireActivity().getSupportFragmentManager(),
            null
        )

        dismiss()
    }

    private fun initPlaylistView() {
        bind!!.playlistDialogRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.playlistDialogRecyclerView.setHasFixedSize(true)

        playlistDialogHorizontalAdapter = PlaylistDialogHorizontalAdapter(this)
        bind!!.playlistDialogRecyclerView.setAdapter(playlistDialogHorizontalAdapter)

        playlistChooserViewModel!!.getPlaylistList(requireActivity())
            .observe(requireActivity(), Observer { playlists: MutableList<Playlist?>? ->
                if (playlists != null) {
                    if (!playlists.isEmpty()) {
                        if (bind != null) bind!!.noPlaylistsCreatedTextView.setVisibility(View.GONE)
                        if (bind != null) bind!!.playlistDialogRecyclerView.setVisibility(View.VISIBLE)
                        playlistDialogHorizontalAdapter!!.setItems(playlists)
                    } else {
                        if (bind != null) bind!!.noPlaylistsCreatedTextView.setVisibility(View.VISIBLE)
                        if (bind != null) bind!!.playlistDialogRecyclerView.setVisibility(View.GONE)
                    }
                }
            })
    }

    override fun onPlaylistClick(bundle: Bundle?) {
        if (playlistChooserViewModel!!.songsToAdd != null && !playlistChooserViewModel!!.songsToAdd.isEmpty()) {
            val playlist = bundle?.getParcelable<Playlist?>(Constants.PLAYLIST_OBJECT)
            playlistChooserViewModel!!.addSongsToPlaylist(this, getDialog()!!, playlist!!.id)
        } else {
            Toast.makeText(
                requireContext(),
                R.string.playlist_chooser_dialog_toast_add_failure,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
