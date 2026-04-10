package com.cappielloantonio.tempo.ui.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentHomeBinding
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.fragment.pager.HomePager
import com.cappielloantonio.tempo.util.Preferences.isPodcastSectionVisible
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import java.util.Objects

@UnstableApi
class HomeFragment : Fragment() {
    private var bind: FragmentHomeBinding? = null
    private var activity: MainActivity? = null

    private var materialToolbar: MaterialToolbar? = null
    private var appBarLayout: AppBarLayout? = null
    private var tabLayout: TabLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = getActivity() as MainActivity?
        bind = FragmentHomeBinding.inflate(inflater, container, false)
        return bind!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAppBar()
        initHomePager()
    }

    override fun onStart() {
        super.onStart()

        activity!!.toggleBottomNavigationBarVisibilityOnOrientationChange()
        activity!!.setBottomSheetVisibility(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun initAppBar() {
        appBarLayout = bind!!.getRoot().findViewById<AppBarLayout>(R.id.toolbar_fragment)
        materialToolbar = bind!!.getRoot().findViewById<MaterialToolbar>(R.id.toolbar)

        activity!!.setSupportActionBar(materialToolbar)
        requireNotNull(materialToolbar!!.overflowIcon)
            .setTint(requireContext().getResources().getColor(R.color.titleTextColor, null))

        tabLayout = TabLayout(requireContext())
        tabLayout!!.setTabGravity(TabLayout.GRAVITY_FILL)
        tabLayout!!.setTabMode(TabLayout.MODE_FIXED)

        appBarLayout!!.addView(tabLayout)
    }

    private fun initHomePager() {
        val pager = HomePager(this)

        pager.addFragment(
            HomeTabMusicFragment(),
            getString(R.string.home_section_music),
            R.drawable.ic_home
        )

        if (isPodcastSectionVisible()) pager.addFragment(
            HomeTabPodcastFragment(),
            getString(R.string.home_section_podcast),
            R.drawable.ic_graphic_eq
        )

        bind!!.homeViewPager.setAdapter(pager)
        bind!!.homeViewPager.setOffscreenPageLimit(3)
        bind!!.homeViewPager.setUserInputEnabled(false)

        TabLayoutMediator(
            tabLayout!!, bind!!.homeViewPager,
            TabConfigurationStrategy { tab: TabLayout.Tab?, position: Int ->
                tab!!.setText(pager.getPageTitle(position))
            }
        ).attach()

        tabLayout!!.setVisibility(if (isPodcastSectionVisible()) View.VISIBLE else View.GONE)
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}
