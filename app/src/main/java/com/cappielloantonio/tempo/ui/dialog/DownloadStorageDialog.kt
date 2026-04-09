package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogDownloadStorageBinding
import com.cappielloantonio.tempo.interfaces.DialogClickCallback
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadStoragePreference
import com.cappielloantonio.tempo.util.Preferences.setDownloadStoragePreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(markerClass = [UnstableApi::class])
class DownloadStorageDialog(private val dialogClickCallback: DialogClickCallback) :
    DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogDownloadStorageBinding.inflate(getLayoutInflater())

        return MaterialAlertDialogBuilder(getActivity()!!)
            .setView(bind.getRoot())
            .setTitle(R.string.download_storage_dialog_title)
            .setPositiveButton(R.string.download_storage_external_dialog_positive_button, null)
            .setNegativeButton(R.string.download_storage_internal_dialog_negative_button, null)
            .setNeutralButton(R.string.download_storage_directory_dialog_neutral_button, null)
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
                val currentPreference = getDownloadStoragePreference()
                val newPreference = 1

                if (currentPreference != newPreference) {
                    setDownloadStoragePreference(newPreference)
                    DownloadUtil.getDownloadTracker(requireContext()).removeAll()
                    dialogClickCallback.onPositiveClick()
                }
                dialog.dismiss()
            })

            val negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener(View.OnClickListener { v: View? ->
                val currentPreference = getDownloadStoragePreference()
                val newPreference = 0

                if (currentPreference != newPreference) {
                    setDownloadStoragePreference(newPreference)
                    DownloadUtil.getDownloadTracker(requireContext()).removeAll()
                    dialogClickCallback.onNegativeClick()
                }
                dialog.dismiss()
            })

            val neutralButton = dialog.getButton(Dialog.BUTTON_NEUTRAL)
            neutralButton.setOnClickListener(View.OnClickListener { v: View? ->
                val currentPreference = getDownloadStoragePreference()
                val newPreference = 2

                if (currentPreference != newPreference) {
                    setDownloadStoragePreference(newPreference)
                    DownloadUtil.getDownloadTracker(requireContext()).removeAll()
                    dialogClickCallback.onNeutralClick()
                }
                dialog.dismiss()
            })
        }
    }
}
