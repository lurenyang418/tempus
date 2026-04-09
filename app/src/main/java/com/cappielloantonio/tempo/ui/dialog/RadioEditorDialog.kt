package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogRadioEditorBinding
import com.cappielloantonio.tempo.interfaces.RadioCallback
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.RadioEditorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Objects

class RadioEditorDialog(private val radioCallback: RadioCallback?) : DialogFragment() {
    private var bind: DialogRadioEditorBinding? = null
    private var radioEditorViewModel: RadioEditorViewModel? = null

    private var radioName: String? = null
    private var radioStreamURL: String? = null
    private var radioHomepageURL: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogRadioEditorBinding.inflate(getLayoutInflater())
        radioEditorViewModel =
            ViewModelProvider(requireActivity()).get<RadioEditorViewModel>(RadioEditorViewModel::class.java)

        setupObservers()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(bind!!.getRoot())
            .setTitle(R.string.radio_editor_dialog_title)
            .setPositiveButton(
                R.string.radio_editor_dialog_positive_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    if (validateInput()) {
                        if (radioEditorViewModel!!.radioToEdit == null) {
                            radioEditorViewModel!!.createRadio(
                                radioName, radioStreamURL,
                                if (radioHomepageURL!!.isEmpty()) null else radioHomepageURL
                            )
                        } else {
                            radioEditorViewModel!!.updateRadio(
                                radioName, radioStreamURL,
                                if (radioHomepageURL!!.isEmpty()) null else radioHomepageURL
                            )
                        }
                    }
                })
            .setNeutralButton(
                R.string.radio_editor_dialog_neutral_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    radioEditorViewModel!!.deleteRadio()
                })
            .setNegativeButton(
                R.string.radio_editor_dialog_negative_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    dialog!!.cancel()
                })
            .create()
    }

    private fun setupObservers() {
        radioEditorViewModel!!.getIsSuccess().observe(this, Observer { isSuccess: Boolean? ->
            if (isSuccess != null && isSuccess) {
                Toast.makeText(
                    requireContext(),
                    if (radioEditorViewModel!!.radioToEdit == null) App.getContext()!!
                        .getString(R.string.radio_editor_dialog_added) else App.getContext()!!
                        .getString(R.string.radio_editor_dialog_updated),
                    Toast.LENGTH_SHORT
                ).show()
                dismissDialog()
            }
        })
        radioEditorViewModel!!.getErrorMessage().observe(this, Observer { error: String? ->
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                radioEditorViewModel!!.clearError()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        setParameterInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setParameterInfo() {
        if (getArguments() != null && getArguments()!!.getParcelable<Parcelable?>(Constants.INTERNET_RADIO_STATION_OBJECT) != null) {
            val toEdit =
                requireArguments().getParcelable<InternetRadioStation?>(Constants.INTERNET_RADIO_STATION_OBJECT)
            radioEditorViewModel!!.radioToEdit = toEdit

            bind!!.internetRadioStationNameTextView.setText(toEdit!!.name)
            bind!!.internetRadioStationStreamUrlTextView.setText(toEdit.streamUrl)
            bind!!.internetRadioStationHomepageUrlTextView.setText(toEdit.homePageUrl)
        }
    }

    private fun validateInput(): Boolean {
        radioName =
            Objects.requireNonNull<Editable?>(bind!!.internetRadioStationNameTextView.getText())
                .toString().trim { it <= ' ' }
        radioStreamURL =
            Objects.requireNonNull<Editable?>(bind!!.internetRadioStationStreamUrlTextView.getText())
                .toString().trim { it <= ' ' }
        radioHomepageURL =
            Objects.requireNonNull<Editable?>(bind!!.internetRadioStationHomepageUrlTextView.getText())
                .toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(radioName)) {
            bind!!.internetRadioStationNameTextView.setError(getString(R.string.error_required))
            return false
        }
        if (TextUtils.isEmpty(radioStreamURL)) {
            bind!!.internetRadioStationStreamUrlTextView.setError(getString(R.string.error_required))
            return false
        }
        return true
    }

    private fun dismissDialog() {
        if (radioCallback != null) {
            radioCallback.onDismiss()
        }
        Objects.requireNonNull<Dialog?>(getDialog()).dismiss()
    }
}