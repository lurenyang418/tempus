package com.cappielloantonio.tempo.ui.fragment.pager

import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cappielloantonio.tempo.ui.fragment.PlayerCoverFragment
import com.cappielloantonio.tempo.ui.fragment.PlayerLyricsFragment

@OptIn(markerClass = [UnstableApi::class])
class PlayerControllerHorizontalPager(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return PlayerCoverFragment()
            1 -> return PlayerLyricsFragment()
        }

        return PlayerCoverFragment()
    }

    override fun getItemCount(): Int {
        return 2
    }

    companion object {
        private const val TAG = "PlayerControllerHorizontalPager"
    }
}