package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentToolbarBinding
import com.cappielloantonio.tempo.ui.activity.MainActivity

@UnstableApi
class ToolbarFragment : Fragment() {
    private var bind: FragmentToolbarBinding? = null
    private var activity: MainActivity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_page_menu, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        bind = FragmentToolbarBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()

        return view
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.action_search) {
            activity?.navController?.navigate(R.id.searchFragment)
            return true
        } else if (item.getItemId() == R.id.action_settings) {
            activity?.navController?.navigate(R.id.settingsFragment)
            return true
        }

        return false
    }

    companion object {
        private const val TAG = "ToolbarFragment"
    }
}