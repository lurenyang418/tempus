package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentSettingsBinding
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Preferences.getEnableDrawerOnPortrait

class SettingsFragment : Fragment() {
    private var activity: MainActivity? = null
    private var bind: FragmentSettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity = getActivity() as MainActivity?
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bind = FragmentSettingsBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()

        initAppBar()

        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Add the PreferenceFragment only the first time
        if (savedInstanceState == null) {
            val prefFragment = SettingsContainerFragment()

            // Use the child fragment manager so the PreferenceFragment is scoped to this fragment
            getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, prefFragment)
                .setReorderingAllowed(true) // optional but recommended
                .commit()
        }
    }

    override fun onStart() {
        super.onStart()
        activity!!.setBottomNavigationBarVisibility(false)
        activity!!.setBottomSheetVisibility(false)
        activity!!.setNavigationDrawerLock(true)
        activity!!.setSystemBarsVisibility(!activity!!.isLandscape)
    }

    override fun onStop() {
        super.onStop()
        activity!!.setBottomSheetVisibility(true)

        if (activity!!.isLandscape) {
            activity!!.setNavigationDrawerLock(false)
        } else if (getEnableDrawerOnPortrait()) {
            activity!!.setNavigationDrawerLock(false)
        }
    }

    private fun initAppBar() {
        bind!!.settingsToolbar.setNavigationOnClickListener(View.OnClickListener { v: View? ->
            activity!!.navController!!.navigateUp()
        })
    }
}
