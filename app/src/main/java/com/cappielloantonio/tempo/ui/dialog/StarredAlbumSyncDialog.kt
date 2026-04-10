package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogStarredAlbumSyncBinding
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.MappingUtil.mapDownloads
import com.cappielloantonio.tempo.util.Preferences.setStarredAlbumsSyncEnabled
import com.cappielloantonio.tempo.viewmodel.StarredAlbumsSyncViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.stream.Collectors

@OptIn(markerClass = [UnstableApi::class])
class StarredAlbumSyncDialog(private val onCancel: Runnable?) : DialogFragment() {
    private var starredAlbumsSyncViewModel: StarredAlbumsSyncViewModel? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogStarredAlbumSyncBinding.inflate(getLayoutInflater())

        starredAlbumsSyncViewModel =
            ViewModelProvider(requireActivity()).get<StarredAlbumsSyncViewModel>(
                StarredAlbumsSyncViewModel::class.java
            )

        return MaterialAlertDialogBuilder(getActivity()!!)
            .setView(bind.getRoot())
            .setTitle(R.string.starred_album_sync_dialog_title)
            .setPositiveButton(R.string.starred_sync_dialog_positive_button, null)
            .setNeutralButton(R.string.starred_sync_dialog_neutral_button, null)
            .setNegativeButton(R.string.starred_sync_dialog_negative_button, null)
            .create()
    }

    override fun onResume() {
        super.onResume()
        setButtonAction(requireContext())
    }

    private fun setButtonAction(context: Context?) {
        val dialog = getDialog() as AlertDialog?

        if (dialog != null) {
            val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener(View.OnClickListener { v: View? ->
                starredAlbumsSyncViewModel!!.getStarredAlbumSongs(requireActivity())
                    .observe(this, Observer { allSongs: MutableList<Child?>? ->
                        if (allSongs != null && !allSongs.isEmpty()) {
                            DownloadUtil.getDownloadTracker(context).download(
                                mapDownloads(allSongs.filterNotNull()),
                                allSongs.filterNotNull().map { Download(it) }.toMutableList()
                            )
                        }
                        dialog.dismiss()
                    })
            })

            val neutralButton = dialog.getButton(Dialog.BUTTON_NEUTRAL)
            neutralButton.setOnClickListener(View.OnClickListener { v: View? ->
                setStarredAlbumsSyncEnabled(true)
                dialog.dismiss()
            })

            val negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener(View.OnClickListener { v: View? ->
                setStarredAlbumsSyncEnabled(false)
                if (onCancel != null) onCancel.run()
                dialog.dismiss()
            })
        }
    }
}