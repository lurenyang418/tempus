package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogGithubTempoUpdateBinding
import com.cappielloantonio.tempo.github.models.LatestRelease
import com.cappielloantonio.tempo.util.Preferences.setTempusUpdateReminder
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GithubTempoUpdateDialog(private val latestRelease: LatestRelease) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogGithubTempoUpdateBinding.inflate(getLayoutInflater())

        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setView(bind.getRoot())
            .setTitle(R.string.github_update_dialog_title)
            .setPositiveButton(
                R.string.github_update_dialog_positive_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> })
            .setNegativeButton(
                R.string.github_update_dialog_negative_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> })
            .setNeutralButton(
                R.string.github_update_dialog_neutral_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> })

        return builder.create()
    }

    override fun onStart() {
        super.onStart()

        setButtonAction()
    }

    private fun setButtonAction() {
        val alertDialog = requireDialog() as AlertDialog

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(View.OnClickListener { v: View? ->
                openLink(latestRelease.htmlUrl)
                dismiss()
            })

        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setOnClickListener(View.OnClickListener { v: View? ->
                setTempusUpdateReminder()
                dismiss()
            })

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setOnClickListener(View.OnClickListener { v: View? ->
                openLink(getString(R.string.support_url))
                dismiss()
            })
    }

    private fun openLink(link: String?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}