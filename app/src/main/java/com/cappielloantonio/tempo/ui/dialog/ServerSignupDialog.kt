package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import android.text.Editable
import android.text.TextUtils
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogServerSignupBinding
import com.cappielloantonio.tempo.model.Server
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.LoginViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Objects
import java.util.UUID

class ServerSignupDialog : DialogFragment() {
    private var bind: DialogServerSignupBinding? = null
    private var loginViewModel: LoginViewModel? = null

    private var serverName: String? = null
    private var username: String? = null
    private var password: String? = null
    private var server: String? = null
    private var localAddress: String? = null
    private var lowSecurity = false
    private var clientCertAlias: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogServerSignupBinding.inflate(getLayoutInflater())
        bind!!.clientCertTextView.setOnClickListener(View.OnClickListener { v: View? ->
            if (TextUtils.isEmpty(bind!!.clientCertTextView.getText())) {
                KeyChain.choosePrivateKeyAlias(
                    requireActivity(),
                    KeyChainAliasCallback { alias: String? ->
                        bind!!.clientCertTextView.setText(alias)
                    },
                    null,
                    null,
                    null,
                    null
                )
            } else {
                bind!!.clientCertTextView.setText(null)
            }
        })

        loginViewModel =
            ViewModelProvider(requireActivity()).get<LoginViewModel>(LoginViewModel::class.java)

        return MaterialAlertDialogBuilder(getActivity()!!)
            .setView(bind!!.getRoot())
            .setTitle(R.string.server_signup_dialog_title)
            .setNeutralButton(
                R.string.server_signup_dialog_neutral_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> })
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

        setServerInfo()
        setButtonAction()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setServerInfo() {
        if (getArguments() != null) {
            loginViewModel!!.serverToEdit =
                requireArguments().getParcelable<Server?>("server_object")

            if (loginViewModel!!.serverToEdit != null) {
                bind!!.serverNameTextView.setText(loginViewModel!!.serverToEdit!!.serverName)
                bind!!.usernameTextView.setText(loginViewModel!!.serverToEdit!!.username)
                bind!!.passwordTextView.setText("")
                bind!!.serverTextView.setText(loginViewModel!!.serverToEdit!!.address)
                bind!!.localAddressTextView.setText(loginViewModel!!.serverToEdit!!.localAddress)
                bind!!.lowSecurityCheckbox.setChecked(loginViewModel!!.serverToEdit!!.isLowSecurity)
                bind!!.clientCertTextView.setText(loginViewModel!!.serverToEdit!!.clientCert)
            }
        } else {
            loginViewModel!!.serverToEdit = null
        }
    }

    private fun setButtonAction() {
        val alertDialog = Objects.requireNonNull<Dialog?>(getDialog()) as AlertDialog

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(View.OnClickListener { v: View? ->
                if (validateInput()) {
                    saveServerPreference()
                    Objects.requireNonNull<Dialog?>(getDialog()).dismiss()
                }
            })

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setOnClickListener(View.OnClickListener { v: View? ->
                Toast.makeText(
                    requireContext(),
                    R.string.server_signup_dialog_action_delete_toast,
                    Toast.LENGTH_SHORT
                ).show()
            })

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setOnLongClickListener(OnLongClickListener { v: View? ->
                loginViewModel!!.deleteServer(null)
                Objects.requireNonNull<Dialog?>(getDialog()).dismiss()
                true
            })
    }

    private fun validateInput(): Boolean {
        serverName =
            Objects.requireNonNull<Editable?>(bind!!.serverNameTextView.getText()).toString()
                .trim { it <= ' ' }
        username = Objects.requireNonNull<Editable?>(bind!!.usernameTextView.getText()).toString()
            .trim { it <= ' ' }
        password = if (bind!!.lowSecurityCheckbox.isChecked()) MusicUtil.passwordHexEncoding(
            Objects.requireNonNull<Editable?>(bind!!.passwordTextView.getText()).toString()
        ) else Objects.requireNonNull<Editable?>(bind!!.passwordTextView.getText()).toString()
        server = if (bind!!.serverTextView.getText() != null && !bind!!.serverTextView.getText()
                .toString().trim { it <= ' ' }.isBlank()
        ) bind!!.serverTextView.getText().toString().trim { it <= ' ' } else null
        localAddress =
            if (bind!!.localAddressTextView.getText() != null && !bind!!.localAddressTextView.getText()
                    .toString().trim { it <= ' ' }.isBlank()
            ) bind!!.localAddressTextView.getText().toString().trim { it <= ' ' } else null
        lowSecurity = bind!!.lowSecurityCheckbox.isChecked()
        clientCertAlias =
            if (bind!!.clientCertTextView.getText() != null && !bind!!.clientCertTextView.getText()
                    .toString().trim { it <= ' ' }.isBlank()
            ) bind!!.clientCertTextView.getText().toString().trim { it <= ' ' } else null

        if (TextUtils.isEmpty(serverName)) {
            bind!!.serverNameTextView.setError(getString(R.string.error_required))
            return false
        }

        if (TextUtils.isEmpty(username)) {
            bind!!.usernameTextView.setError(getString(R.string.error_required))
            return false
        }

        if (TextUtils.isEmpty(server)) {
            bind!!.serverTextView.setError(getString(R.string.error_required))
            return false
        }

        if (!TextUtils.isEmpty(localAddress) && !localAddress!!.matches("^https?://(.*)".toRegex())) {
            bind!!.localAddressTextView.setError(getString(R.string.error_server_prefix))
            return false
        }

        if (!server!!.matches("^https?://(.*)".toRegex())) {
            bind!!.serverTextView.setError(getString(R.string.error_server_prefix))
            return false
        }

        return true
    }

    private fun saveServerPreference() {
        val serverID =
            if (loginViewModel!!.serverToEdit != null) loginViewModel!!.serverToEdit!!.serverId else UUID.randomUUID()
                .toString()
        loginViewModel!!.addServer(
            Server(
                serverID,
                this.serverName!!,
                this.username!!,
                this.password!!,
                this.server!!,
                this.localAddress,
                System.currentTimeMillis(),
                this.lowSecurity,
                this.clientCertAlias
            )
        )
    }

    companion object {
        private const val TAG = "ServerSignupDialog"
    }
}
