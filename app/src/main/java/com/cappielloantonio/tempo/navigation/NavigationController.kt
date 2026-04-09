package com.cappielloantonio.tempo.navigation

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.google.android.material.bottomsheet.BottomSheetBehavior

class NavigationController(var helper: NavigationHelper) {
    fun syncWithBottomSheetBehavior(
        bottomSheetBehavior: BottomSheetBehavior<View?>,
        navController: NavController
    ) {
        helper.syncWithBottomSheetBehavior(bottomSheetBehavior, navController)
    }

    fun setNavbarVisibility(visibility: Boolean) {
        helper.setBottomNavigationBarVisibility(visibility)
    }

    fun setDrawerLock(visibility: Boolean) {
        helper.setNavigationDrawerLock(visibility)
    }

    val isNavigationDrawerLocked: Boolean
        get() = helper.isNavigationDrawerLocked

    fun toggleDrawerLockOnOrientation(activity: AppCompatActivity) {
        helper.toggleNavigationDrawerLockOnOrientationChange(activity)
    }

    fun setSystemBarsVisibility(activity: AppCompatActivity, visibility: Boolean) {
        helper.setSystemBarsVisibility(activity, visibility)
    }
}