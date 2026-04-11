package com.cappielloantonio.tempo.ui.controller

import android.os.Handler
import android.view.View
import androidx.fragment.app.FragmentManager
import com.cappielloantonio.tempo.ui.fragment.PlayerBottomSheetFragment
import com.cappielloantonio.tempo.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import kotlin.math.max
import kotlin.math.min

class BottomSheetHelper(
    var bottomSheetBehavior: BottomSheetBehavior<View?>,
    var bottomSheetView: View,
// Of the entire activity
    var fragmentManager: FragmentManager
) {
    var playerBottomSheetFragment: PlayerBottomSheetFragment?

    init {
        this.playerBottomSheetFragment = PlayerBottomSheetFragment()
    }

    fun addCallback(callback: BottomSheetCallback) {
        bottomSheetBehavior.addBottomSheetCallback(callback)
    }

    fun setStateInPeek(isVisible: Boolean) {
        if (isVisible) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    fun setVisibility(visibility: Boolean) {
        if (visibility) {
            bottomSheetView.setVisibility(View.VISIBLE)
        } else {
            bottomSheetView.setVisibility(View.GONE)
        }
    }

    fun replaceFragment(playerBottomSheet: Int) {
        fragmentManager
            .beginTransaction()
            .replace(
                playerBottomSheet,
                playerBottomSheetFragment!!,
                "PlayerBottomSheet"
            )
            .commit()
    }

    fun checkAfterStateChanged(mainViewModel: MainViewModel) {
        val handler = Handler()
        val runnable = Runnable { setStateInPeek(mainViewModel.isQueueLoaded) }
        handler.postDelayed(runnable, 100)
    }

    fun collapseDelayed() {
        val handler = Handler()
        val runnable =
            Runnable { bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED) }
        handler.postDelayed(runnable, 100)
    }

    fun setDraggable(isDraggable: Boolean) {
        bottomSheetBehavior.setDraggable((isDraggable))
    }

    var state: Int
        get() = bottomSheetBehavior.getState()
        set(state) {
            bottomSheetBehavior.setState(state)
        }

    fun animate(slideOffset: Float) {
        if (playerBottomSheetFragment != null) {
            val condensedSlideOffset = max(0.0f, min(0.2f, slideOffset - 0.2f)) / 0.2f
                playerBottomSheetFragment!!.playerHeader.setAlpha(1 - condensedSlideOffset)
                playerBottomSheetFragment!!.playerHeader.setVisibility(if (condensedSlideOffset > 0.99) View.GONE else View.VISIBLE)
        }
    }

    fun setPeekHeight(peekHeight: Int, displayDensity: Float) {
        val newPeekPx = (peekHeight * displayDensity).toInt()
        bottomSheetBehavior.setPeekHeight(newPeekPx)
    }
}