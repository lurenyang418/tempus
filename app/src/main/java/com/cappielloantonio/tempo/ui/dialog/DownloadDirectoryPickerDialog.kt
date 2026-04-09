package com.cappielloantonio.tempo.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.DialogFragment
import com.cappielloantonio.tempo.util.ExternalAudioReader.refreshCache
import com.cappielloantonio.tempo.util.Preferences.setDownloadDirectoryUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DownloadDirectoryPickerDialog : DialogFragment() {
    private var folderPickerLauncher: ActivityResultLauncher<Intent?>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Register launcher *before* button triggers
        folderPickerLauncher = registerForActivityResult<Intent?, ActivityResult?>(
            StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? ->
                if (result!!.getResultCode() == Activity.RESULT_OK) {
                    val data = result.getData()
                    if (data != null) {
                        val uri = data.getData()
                        if (uri != null) {
                            requireContext().getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )

                            setDownloadDirectoryUri(uri.toString())
                            refreshCache()

                            Toast.makeText(
                                requireContext(),
                                "Download directory set:\n" + uri.toString(),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Download Directory")
            .setMessage("Choose a folder where downloaded songs will be stored.")
            .setPositiveButton(
                "Choose Folder",
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addFlags(
                        (Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    )
                    folderPickerLauncher!!.launch(intent)
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}
