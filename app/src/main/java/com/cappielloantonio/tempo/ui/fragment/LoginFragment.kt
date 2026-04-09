package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentLoginBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.interfaces.SystemCallback
import com.cappielloantonio.tempo.model.Server
import com.cappielloantonio.tempo.repository.SystemRepository
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.ServerAdapter
import com.cappielloantonio.tempo.ui.dialog.ServerSignupDialog
import com.cappielloantonio.tempo.util.Preferences.setClientCert
import com.cappielloantonio.tempo.util.Preferences.setLocalAddress
import com.cappielloantonio.tempo.util.Preferences.setLowSecurity
import com.cappielloantonio.tempo.util.Preferences.setPassword
import com.cappielloantonio.tempo.util.Preferences.setSalt
import com.cappielloantonio.tempo.util.Preferences.setServer
import com.cappielloantonio.tempo.util.Preferences.setServerId
import com.cappielloantonio.tempo.util.Preferences.setToken
import com.cappielloantonio.tempo.util.Preferences.setUser
import com.cappielloantonio.tempo.util.Preferences.switchInUseServerAddress
import com.cappielloantonio.tempo.viewmodel.LoginViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener

@UnstableApi
class LoginFragment : Fragment(), ClickCallback {
    private var bind: FragmentLoginBinding? = null
    private var activity: MainActivity? = null
    private var loginViewModel: LoginViewModel? = null

    private var serverAdapter: ServerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.login_page_menu, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = getActivity() as MainActivity?

        loginViewModel =
            ViewModelProvider(requireActivity()).get<LoginViewModel>(LoginViewModel::class.java)
        bind = FragmentLoginBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()

        initAppBar()
        initServerListView()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun initAppBar() {
        activity!!.setSupportActionBar(bind!!.toolbar)

        bind!!.appBarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout: AppBarLayout?, verticalOffset: Int ->
            if ((bind!!.serverInfoSector.getHeight() + verticalOffset) < (2 * ViewCompat.getMinimumHeight(
                    bind!!.toolbar
                ))
            ) {
                bind!!.toolbar.setTitle(R.string.login_title)
            } else {
                bind!!.toolbar.setTitle(R.string.empty_string)
            }
        })
    }

    private fun initServerListView() {
        bind!!.serverListRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.serverListRecyclerView.setHasFixedSize(true)

        serverAdapter = ServerAdapter(this)
        bind!!.serverListRecyclerView.setAdapter(serverAdapter)
        loginViewModel!!.serverList!!.observe(
            getViewLifecycleOwner(),
            Observer { servers: MutableList<Server?>? ->
                if (!servers.isNullOrEmpty()) {
                    if (bind != null) bind!!.noServerAddedTextView.setVisibility(View.GONE)
                    if (bind != null) bind!!.serverListRecyclerView.setVisibility(View.VISIBLE)
                    @Suppress("UNCHECKED_CAST")
                    serverAdapter!!.setItems(servers as MutableList<Server>)
                } else {
                    if (bind != null) bind!!.noServerAddedTextView.setVisibility(View.VISIBLE)
                    if (bind != null) bind!!.serverListRecyclerView.setVisibility(View.GONE)
                }
            })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.action_add) {
            val dialog = ServerSignupDialog()
            dialog.show(activity!!.getSupportFragmentManager(), null)
            return true
        }

        return false
    }

    override fun onServerClick(bundle: Bundle?) {
        val server = bundle?.getParcelable<Server?>("server_object")
        saveServerPreference(
            server!!.serverId,
            server.address,
            server.localAddress,
            server.username,
            server.password,
            server.isLowSecurity,
            server.clientCert
        )

        val systemRepository = SystemRepository()
        systemRepository.checkUserCredential(object : SystemCallback {
            override fun onError(exception: Exception?) {
                switchInUseServerAddress()
                resetServerPreference()
                Toast.makeText(requireContext(), exception?.message, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess(password: String?, token: String?, salt: String?) {
                activity!!.goFromLogin()
            }
        })
    }

    override fun onServerLongClick(bundle: Bundle?) {
        val dialog = ServerSignupDialog()
        dialog.setArguments(bundle)
        dialog.show(activity!!.getSupportFragmentManager(), null)
    }

    private fun saveServerPreference(
        serverId: String?,
        server: String?,
        localAddress: String?,
        user: String?,
        password: String?,
        isLowSecurity: Boolean,
        clientCert: String?
    ) {
        setServerId(serverId)
        setServer(server)
        setLocalAddress(localAddress)
        setUser(user)
        setPassword(password)
        setLowSecurity(isLowSecurity)
        setClientCert(clientCert)

        getSubsonicClientInstance(true)
    }

    private fun resetServerPreference() {
        setServerId(null)
        setServer(null)
        setUser(null)
        setPassword(null)
        setToken(null)
        setSalt(null)
        setLowSecurity(false)
        setClientCert(null)

        getSubsonicClientInstance(true)
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}
