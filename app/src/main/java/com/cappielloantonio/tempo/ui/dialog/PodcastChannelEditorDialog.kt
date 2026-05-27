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
import com.cappielloantonio.tempo.databinding.DialogPodcastChannelEditorBinding
import com.cappielloantonio.tempo.interfaces.PodcastCallback
import com.cappielloantonio.tempo.viewmodel.PodcastChannelEditorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PodcastChannelEditorDialog(private val podcastCallback: PodcastCallback) : DialogFragment() {
    private var bind: DialogPodcastChannelEditorBinding? = null
    private var podcastChannelEditorViewModel: PodcastChannelEditorViewModel? = null

    private var channelUrl: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogPodcastChannelEditorBinding.inflate(getLayoutInflater())

        podcastChannelEditorViewModel =
            ViewModelProvider(requireActivity()).get<PodcastChannelEditorViewModel>(
                PodcastChannelEditorViewModel::class.java
            )

        return MaterialAlertDialogBuilder(getActivity()!!)
            .setView(bind!!.getRoot())
            .setTitle(R.string.podcast_channel_editor_dialog_title)
            .setPositiveButton(
                R.string.server_signup_dialog_positive_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> })
            .setNegativeButton(
                R.string.server_signup_dialog_negative_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })
            .create()
    }

    override fun onStart() {
        super.onStart()

        setButtonAction()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setButtonAction() {
        val dialog = getDialog() as AlertDialog?
        if (dialog != null) {
            val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener(View.OnClickListener { v: View? ->
                if (validateInput()) {
                    podcastChannelEditorViewModel!!.createChannel(channelUrl)
                    dismissDialog()
                }
            })
        }
    }


    private fun validateInput(): Boolean {
        channelUrl = bind!!.podcastChannelRssUrlNameTextView.text?.toString()?.trim { it <= ' ' }

        if (TextUtils.isEmpty(channelUrl)) {
            bind!!.podcastChannelRssUrlNameTextView.setError(getString(R.string.error_required))
            return false
        }

        return true
    }

    private fun dismissDialog() {
        podcastCallback.onDismiss()
        dismiss()
    }
}
