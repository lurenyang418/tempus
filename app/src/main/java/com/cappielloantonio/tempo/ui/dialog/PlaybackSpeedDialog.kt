package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.Preferences.getPlaybackSpeed
import com.cappielloantonio.tempo.util.Preferences.setPlaybackSpeed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.abs

class PlaybackSpeedDialog : DialogFragment() {
    interface PlaybackSpeedListener {
        fun onSpeedSelected(speed: Float)
    }

    private var listener: PlaybackSpeedListener? = null

    fun setPlaybackSpeedListener(listener: PlaybackSpeedListener?) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val currentSpeed = getPlaybackSpeed()
        val selectedIndex = getSelectedIndex(currentSpeed)

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.playback_speed_dialog_title)
            .setSingleChoiceItems(
                SPEED_LABELS,
                selectedIndex,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    val selectedSpeed: Float = SPEED_VALUES[which]
                    setPlaybackSpeed(selectedSpeed)
                    if (listener != null) {
                        listener!!.onSpeedSelected(selectedSpeed)
                    }
                    dialog!!.dismiss()
                })
            .setNegativeButton(
                R.string.playback_speed_dialog_negative_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })
            .create()
    }

    private fun getSelectedIndex(currentSpeed: Float): Int {
        for (i in SPEED_VALUES.indices) {
            if (abs(SPEED_VALUES[i] - currentSpeed) < 0.01f) {
                return i
            }
        }
        return 2 // Default to 1.0x
    }

    companion object {
        private const val TAG = "PlaybackSpeedDialog"

        private val SPEED_VALUES = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        private val SPEED_LABELS =
            arrayOf<String?>("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x")
    }
}
