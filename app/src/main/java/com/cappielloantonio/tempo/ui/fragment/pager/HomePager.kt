package com.cappielloantonio.tempo.ui.fragment.pager

import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter

@OptIn(markerClass = [UnstableApi::class])
class HomePager(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val fragments: MutableList<Fragment> = ArrayList<Fragment>()
    private val titles: MutableList<String?> = ArrayList<String?>()
    private val icons: MutableList<Int?> = ArrayList<Int?>()

    override fun createFragment(position: Int): Fragment {
        return fragments.get(position)
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    fun addFragment(fragment: Fragment?, title: String?, drawable: Int) {
        fragments.add(fragment!!)
        titles.add(title)
        icons.add(drawable)
    }

    fun getPageTitle(position: Int): String? {
        return titles.get(position)
    }

    fun getPageIcon(position: Int): Int? {
        return icons.get(position)
    }

    companion object {
        private const val TAG = "HomePager"
    }
}