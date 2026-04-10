package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogPlaylistEditorBinding
import com.cappielloantonio.tempo.interfaces.PlaylistCallback
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.ui.adapter.PlaylistDialogSongHorizontalAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences.isSharingEnabled
import com.cappielloantonio.tempo.viewmodel.PlaylistEditorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Collections

class PlaylistEditorDialog(private val playlistCallback: PlaylistCallback?) : DialogFragment() {
    private var bind: DialogPlaylistEditorBinding? = null
    private var playlistEditorViewModel: PlaylistEditorViewModel? = null

    private var playlistName: String? = null
    private var playlistDialogSongHorizontalAdapter: PlaylistDialogSongHorizontalAdapter? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogPlaylistEditorBinding.inflate(getLayoutInflater())

        playlistEditorViewModel = ViewModelProvider(requireActivity()).get<PlaylistEditorViewModel>(
            PlaylistEditorViewModel::class.java
        )

        return MaterialAlertDialogBuilder(getActivity()!!)
            .setView(bind!!.getRoot())
            .setTitle(R.string.playlist_editor_dialog_title)
            .setPositiveButton(
                R.string.playlist_editor_dialog_positive_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> })
            .setNeutralButton(
                R.string.playlist_editor_dialog_neutral_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })
            .setNegativeButton(
                R.string.playlist_editor_dialog_negative_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })
            .create()
    }

    override fun onStart() {
        super.onStart()

        setParameterInfo()
        setButtonAction()
        initSongsView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setParameterInfo() {
        if (requireArguments().getParcelableArrayList<Parcelable?>(Constants.TRACKS_OBJECT) != null) {
            playlistEditorViewModel!!.songsToAdd =
                requireArguments().getParcelableArrayList<Child?>(
                    Constants.TRACKS_OBJECT
                )!!
            playlistEditorViewModel!!.playlistToEdit = null
        } else if (requireArguments().getParcelable<Parcelable?>(Constants.PLAYLIST_OBJECT) != null) {
            playlistEditorViewModel!!.songsToAdd = arrayListOf()
            playlistEditorViewModel!!.playlistToEdit = requireArguments().getParcelable<Playlist?>(
                Constants.PLAYLIST_OBJECT
            )

            if (playlistEditorViewModel!!.playlistToEdit != null) {
                bind!!.playlistNameTextView.setText(playlistEditorViewModel!!.playlistToEdit!!.name)
            }
        }
    }

    private fun setButtonAction() {
        val alertDialog = requireDialog() as AlertDialog

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(View.OnClickListener { v: View? ->
                if (validateInput()) {
                    if (playlistEditorViewModel!!.songsToAdd != null) {
                        playlistEditorViewModel!!.createPlaylist(playlistName)
                    } else if (playlistEditorViewModel!!.playlistToEdit != null) {
                        playlistEditorViewModel!!.updatePlaylist(playlistName)
                    }

                    dialogDismiss()
                }
            })

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setOnClickListener(View.OnClickListener { v: View? ->
                Toast.makeText(
                    requireContext(),
                    R.string.playlist_editor_dialog_action_delete_toast,
                    Toast.LENGTH_SHORT
                ).show()
            })

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setOnLongClickListener(OnLongClickListener { v: View? ->
                playlistEditorViewModel!!.deletePlaylist()
                dialogDismiss()
                false
            })

        bind!!.playlistShareButton.setOnClickListener(View.OnClickListener { view: View? ->
            playlistEditorViewModel!!.sharePlaylist()
                .observe(requireActivity(), Observer { sharedPlaylist: Share? ->
                    val clipboardManager =
                        requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData =
                        ClipData.newPlainText(getString(R.string.app_name), sharedPlaylist!!.url)
                    clipboardManager.setPrimaryClip(clipData)
                })
        })

        bind!!.playlistShareButton.setVisibility(if (isSharingEnabled()) View.VISIBLE else View.GONE)
    }

    private fun initSongsView() {
        bind!!.playlistSongRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.playlistSongRecyclerView.setHasFixedSize(true)

        playlistDialogSongHorizontalAdapter = PlaylistDialogSongHorizontalAdapter()
        bind!!.playlistSongRecyclerView.setAdapter(playlistDialogSongHorizontalAdapter)

        playlistEditorViewModel!!.playlistSongLiveList.observe(
            requireActivity(),
            Observer { songs: MutableList<Child>? ->
                if (songs != null) playlistDialogSongHorizontalAdapter!!.items = songs.map { it }.toMutableList()
            })

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            var originalPosition: Int = -1
            var fromPosition: Int = -1
            var toPosition: Int = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (originalPosition == -1) originalPosition =
                    viewHolder.getBindingAdapterPosition()

                fromPosition = viewHolder.getBindingAdapterPosition()
                toPosition = target.getBindingAdapterPosition()

                Collections.swap(
                    playlistDialogSongHorizontalAdapter!!.items,
                    fromPosition,
                    toPosition
                )
                requireNotNull(recyclerView.adapter)
                    .notifyItemMoved(fromPosition, toPosition)

                return false
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)

                /*
                 * Qui vado a riscivere tutta la table Queue, quando teoricamente potrei solo swappare l'ordine degli elementi interessati
                 * Nel caso la coda contenesse parecchi brani, potrebbero verificarsi rallentamenti pesanti
                 */
                playlistEditorViewModel!!.orderPlaylistSongLiveListAfterSwap(
                    playlistDialogSongHorizontalAdapter!!.items
                )

                originalPosition = -1
                fromPosition = -1
                toPosition = -1
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                playlistEditorViewModel!!.removeFromPlaylistSongLiveList(viewHolder.getBindingAdapterPosition())
                requireNotNull(bind!!.playlistSongRecyclerView.adapter)
                    .notifyItemRemoved(viewHolder.getBindingAdapterPosition())
            }
        }
        ).attachToRecyclerView(bind!!.playlistSongRecyclerView)
    }

    private fun validateInput(): Boolean {
        playlistName = bind!!.playlistNameTextView.text?.toString()?.trim { it <= ' ' }

        if (TextUtils.isEmpty(playlistName)) {
            bind!!.playlistNameTextView.setError(getString(R.string.error_required))
            return false
        }

        return true
    }

    private fun dialogDismiss() {
        dismiss()
        if (playlistCallback != null) {
            playlistCallback.onDismiss()
        }
    }
}
