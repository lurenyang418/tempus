package com.cappielloantonio.tempo.ui.controller

import com.cappielloantonio.tempo.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback

class BottomSheetController(var helper: BottomSheetHelper) {
    fun expand() {
        helper.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hide() {
        helper.state = BottomSheetBehavior.STATE_HIDDEN
    }

    fun setStateInPeek(isVisible: Boolean) {
        helper.setStateInPeek(isVisible)
    }

    fun setVisibility(visibility: Boolean) {
        helper.setVisibility(visibility)
    }

    fun addCallback(callback: BottomSheetCallback) {
        helper.addCallback(callback)
    }

    fun replaceFragment(playerBottomSheet: Int) {
        helper.replaceFragment(playerBottomSheet)
    }

    fun checkAfterStateChanged(mainViewModel: MainViewModel) {
        helper.checkAfterStateChanged(mainViewModel)
    }

    fun collapseDelayed() {
        helper.collapseDelayed()
    }

    fun setDraggable(isDraggable: Boolean) {
        helper.setDraggable(isDraggable)
    }

    val state: Int
        get() = helper.state

    fun animate(slideOffset: Float) {
        helper.animate(slideOffset)
    }

    fun setPeekHeight(peekHeight: Int, displayDensity: Float) {
        helper.setPeekHeight(peekHeight, displayDensity)
    }
}
