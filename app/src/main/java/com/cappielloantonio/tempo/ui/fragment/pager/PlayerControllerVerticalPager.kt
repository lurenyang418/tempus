package com.cappielloantonio.tempo.ui.fragment.pager

import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cappielloantonio.tempo.ui.fragment.PlayerControllerFragment
import com.cappielloantonio.tempo.ui.fragment.PlayerQueueFragment

@OptIn(markerClass = [UnstableApi::class])
class PlayerControllerVerticalPager(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val maps: HashMap<Int?, Fragment?>

    init {
        this.maps = HashMap<Int?, Fragment?>()
    }

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> {
                val playerControllerFragment: Fragment = PlayerControllerFragment()
                maps.put(position, playerControllerFragment)
                return playerControllerFragment
            }

            1 -> {
                val playerQueueFragment: Fragment = PlayerQueueFragment()
                maps.put(position, playerQueueFragment)
                return playerQueueFragment
            }
        }

        val playerControllerFragment: Fragment = PlayerControllerFragment()
        maps.put(position, playerControllerFragment)
        return playerControllerFragment
    }

    override fun getItemCount(): Int {
        return 2
    }

    fun getRegisteredFragment(position: Int): Fragment? {
        return maps.get(position)
    }
}