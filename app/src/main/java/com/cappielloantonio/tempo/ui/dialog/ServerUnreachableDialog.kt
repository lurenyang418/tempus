package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogServerUnreachableBinding
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Preferences.setServerUnreachableDatetime
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Objects

@OptIn(markerClass = [UnstableApi::class])
class ServerUnreachableDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogServerUnreachableBinding.inflate(getLayoutInflater())

        val popup = MaterialAlertDialogBuilder(getActivity()!!).setView(bind.getRoot())
            .setTitle(R.string.server_unreachable_dialog_title)
            .setPositiveButton(R.string.server_unreachable_dialog_positive_button, null)
            .setNeutralButton(R.string.server_unreachable_dialog_neutral_button, null)
            .setNegativeButton(
                R.string.server_unreachable_dialog_negative_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })
            .create()

        popup.setCanceledOnTouchOutside(false)
        popup.setCancelable(false)

        return popup
    }


    override fun onStart() {
        super.onStart()

        setButtonAction()
    }

    private fun setButtonAction() {
        val alertDialog = Objects.requireNonNull<Dialog?>(getDialog()) as AlertDialog

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setOnClickListener(View.OnClickListener { v: View? ->
                val activity = getActivity() as MainActivity?
                if (activity != null) activity.quit()
                alertDialog.dismiss()
            })

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(View.OnClickListener { v: View? ->
                setServerUnreachableDatetime()
                alertDialog.dismiss()
            })
    }
}
