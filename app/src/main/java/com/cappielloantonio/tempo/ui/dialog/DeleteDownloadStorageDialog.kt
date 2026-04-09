package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogDeleteDownloadStorageBinding
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader.refreshCache
import com.cappielloantonio.tempo.util.ExternalDownloadMetadataStore.clear
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(markerClass = [UnstableApi::class])
class DeleteDownloadStorageDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogDeleteDownloadStorageBinding.inflate(getLayoutInflater())

        return MaterialAlertDialogBuilder(requireContext())
            .setView(bind.getRoot())
            .setTitle(R.string.delete_download_storage_dialog_title)
            .setPositiveButton(R.string.delete_download_storage_dialog_positive_button, null)
            .setNegativeButton(R.string.delete_download_storage_dialog_negative_button, null)
            .create()
    }

    override fun onResume() {
        super.onResume()
        setButtonAction()
    }

    private fun setButtonAction() {
        val dialog = getDialog() as AlertDialog?

        if (dialog != null) {
            val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener(View.OnClickListener { v: View? ->
                if (getDownloadDirectoryUri() == null) {
                    DownloadUtil.getDownloadTracker(requireContext()).removeAll()
                }
                val uriString = getDownloadDirectoryUri()
                if (uriString != null) {
                    val directory = DocumentFile.fromTreeUri(requireContext(), Uri.parse(uriString))
                    if (directory != null && directory.canWrite()) {
                        for (file in directory.listFiles()) {
                            file.delete()
                        }
                    }
                    refreshCache()
                    clear()
                }
                dialog.dismiss()
            })

            val negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener(View.OnClickListener { v: View? ->
                dialog.dismiss()
            })
        }
    }
}
