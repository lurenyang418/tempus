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
import com.cappielloantonio.tempo.databinding.DialogStarredSyncBinding
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.MappingUtil.mapDownloads
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.setStarredSyncEnabled
import com.cappielloantonio.tempo.viewmodel.StarredSyncViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.stream.Collectors

@OptIn(markerClass = [UnstableApi::class])
class StarredSyncDialog(private val onCancel: Runnable?) : DialogFragment() {
    private var starredSyncViewModel: StarredSyncViewModel? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogStarredSyncBinding.inflate(getLayoutInflater())

        starredSyncViewModel =
            ViewModelProvider(requireActivity()).get<StarredSyncViewModel>(StarredSyncViewModel::class.java)

        return MaterialAlertDialogBuilder(getActivity()!!)
            .setView(bind.getRoot())
            .setTitle(R.string.starred_sync_dialog_title)
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
                starredSyncViewModel!!.getStarredTracks(requireActivity())
                    .observe(requireActivity(), Observer { songs: MutableList<Child?>? ->
                        if (songs != null && getDownloadDirectoryUri() == null) {
                            DownloadUtil.getDownloadTracker(context).download(
                                mapDownloads(songs.filterNotNull()),
                                songs.stream().map<Download> { child: Child? -> Download(child!!) }
                                    .collect(
                                        Collectors.toList()
                                    )
                            )
                        }
                        dialog.dismiss()
                    })
            })

            val neutralButton = dialog.getButton(Dialog.BUTTON_NEUTRAL)
            neutralButton.setOnClickListener(View.OnClickListener { v: View? ->
                setStarredSyncEnabled(true)
                dialog.dismiss()
            })

            val negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener(View.OnClickListener { v: View? ->
                setStarredSyncEnabled(false)
                if (onCancel != null) onCancel.run()
                dialog.dismiss()
            })
        }
    }
}
