package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogBatteryOptimizationBinding
import com.cappielloantonio.tempo.util.Preferences.dontAskForOptimization
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(markerClass = [UnstableApi::class])
class BatteryOptimizationDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogBatteryOptimizationBinding.inflate(getLayoutInflater())

        return MaterialAlertDialogBuilder(requireContext())
            .setView(bind.getRoot())
            .setTitle(R.string.activity_battery_optimizations_title)
            .setPositiveButton(
                R.string.battery_optimization_positive_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, listener: Int -> openPowerSettings() })
            .setNeutralButton(
                R.string.battery_optimization_neutral_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, listener: Int -> dontAskForOptimization() })
            .setNegativeButton(R.string.battery_optimization_negative_button, null)
            .create()
    }

    private fun openPowerSettings() {
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        startActivity(intent)
    }
}
