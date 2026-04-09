package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogConnectionAlertBinding
import com.cappielloantonio.tempo.util.Preferences.isDataSavingMode
import com.cappielloantonio.tempo.util.Preferences.setDataSavingMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Objects

class ConnectionAlertDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogConnectionAlertBinding.inflate(getLayoutInflater())

        val builder = MaterialAlertDialogBuilder(getActivity()!!)
            .setView(bind.getRoot())
            .setTitle(R.string.connection_alert_dialog_title)
            .setPositiveButton(
                R.string.connection_alert_dialog_positive_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })
            .setNegativeButton(
                R.string.connection_alert_dialog_negative_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })

        if (!isDataSavingMode()) {
            builder.setNeutralButton(
                R.string.connection_alert_dialog_neutral_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> })
        }

        return builder.create()
    }

    override fun onStart() {
        super.onStart()

        setButtonAction()
    }

    private fun setButtonAction() {
        val alertDialog = Objects.requireNonNull<Dialog?>(getDialog()) as AlertDialog

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setOnClickListener(View.OnClickListener { v: View? ->
                setDataSavingMode(true)
                Objects.requireNonNull<Dialog?>(getDialog()).dismiss()
            })
    }
}