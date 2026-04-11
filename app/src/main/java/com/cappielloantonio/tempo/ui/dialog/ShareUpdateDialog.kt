package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogShareUpdateBinding
import com.cappielloantonio.tempo.util.UIUtil.getReadableDate
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import com.cappielloantonio.tempo.viewmodel.ShareBottomSheetViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Date

class ShareUpdateDialog : DialogFragment() {
    private var bind: DialogShareUpdateBinding? = null
    private var homeViewModel: HomeViewModel? = null
    private var shareBottomSheetViewModel: ShareBottomSheetViewModel? = null

    private var datePicker: MaterialDatePicker<Long?>? = null

    private var descriptionTextView: String? = null
    private var expirationTextView: String? = null
    private var expiration: Long = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        homeViewModel =
            ViewModelProvider(requireActivity()).get<HomeViewModel>(HomeViewModel::class.java)

        shareBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<ShareBottomSheetViewModel>(
                ShareBottomSheetViewModel::class.java
            )

        bind = DialogShareUpdateBinding.inflate(getLayoutInflater())

        return MaterialAlertDialogBuilder(requireContext())
            .setView(bind!!.getRoot())
            .setTitle(R.string.share_update_dialog_title)
            .setPositiveButton(
                R.string.share_update_dialog_positive_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> })
            .setNegativeButton(
                R.string.share_update_dialog_negative_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })
            .create()
    }

    override fun onStart() {
        super.onStart()

        setShareInfo()
        setShareCalendar()
        setButtonAction()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setShareInfo() {
        val share = shareBottomSheetViewModel!!.getShare()
        bind!!.shareDescriptionTextView.setText(share.description)
        // bind.shareExpirationTextView.setText(share.getExpires());
    }

    private fun setShareCalendar() {
        expiration = shareBottomSheetViewModel!!.getShare().expires!!.getTime()

        bind!!.shareExpirationTextView.setText(getReadableDate(Date(expiration)))

        bind!!.shareExpirationTextView.setFocusable(false)
        bind!!.shareExpirationTextView.setOnLongClickListener(null)

        bind!!.shareExpirationTextView.setOnClickListener(View.OnClickListener { view: View? ->
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build()
            datePicker = MaterialDatePicker.Builder.datePicker()
                .setCalendarConstraints(constraints)
                .setSelection(expiration)
                .build()

            datePicker!!.addOnPositiveButtonClickListener(
                MaterialPickerOnPositiveButtonClickListener { selection: Long? ->
                    expiration = selection!!
                    bind!!.shareExpirationTextView.setText(getReadableDate(Date(selection)))
                })
            datePicker!!.show(requireActivity().getSupportFragmentManager(), null)
        })
    }

    private fun setButtonAction() {
        (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(
                View.OnClickListener { v: View? ->
                    if (validateInput()) {
                        updateShare()
                        dismiss()
                    }
                })
    }

    private fun validateInput(): Boolean {
        descriptionTextView = bind!!.shareDescriptionTextView.text?.toString()?.trim { it <= ' ' }
        expirationTextView = bind!!.shareExpirationTextView.text?.toString()?.trim { it <= ' ' }

        if (TextUtils.isEmpty(descriptionTextView)) {
            bind!!.shareDescriptionTextView.setError(getString(R.string.error_required))
            return false
        }

        if (TextUtils.isEmpty(expirationTextView)) {
            bind!!.shareExpirationTextView.setError(getString(R.string.error_required))
            return false
        }

        return true
    }

    private fun updateShare() {
        shareBottomSheetViewModel!!.updateShare(descriptionTextView, expiration)
        homeViewModel!!.refreshShares(requireActivity())
    }
}
