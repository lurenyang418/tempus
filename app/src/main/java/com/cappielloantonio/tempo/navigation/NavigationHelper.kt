package com.cappielloantonio.tempo.navigation

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.Preferences.getEnableDrawerOnPortrait
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import org.jetbrains.annotations.Contract

class NavigationHelper(/*
   All of these are the "backward compatible" changes that don't break the assumption
   that everything was defined on the activity and is gobally available
    */
                       /* UI components */val bottomNavigationView: BottomNavigationView,
                       val bottomNavigationViewFrame: FrameLayout,
                       val drawerLayout: DrawerLayout,
                       /* Navigation components */private val navigationView: NavigationView,
                       navHostFragment: NavHostFragment
) {
    private val navHostFragment: NavHostFragment?

    /* States that need to be remembered */ // -- //
    /* Private constructor */
    init {
        this.navHostFragment = navHostFragment
    }

    fun syncWithBottomSheetBehavior(
        bottomSheetBehavior: BottomSheetBehavior<View?>,
        navController: NavController
    ) {
        navController.addOnDestinationChangedListener(
            OnDestinationChangedListener { controller: NavController?, destination: NavDestination?, arguments: Bundle? ->
                // React to the user clicking one of these on bottom-navbar/drawer
                val isTarget: Boolean = Companion.isTargetDestination(destination!!)
                val currentState = bottomSheetBehavior.getState()
                if (isTarget && currentState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
                }
            })

        setupWithNavController(bottomNavigationView, navController)
        setupWithNavController(navigationView, navController)
    }

    /*
    Clean public methods
    Removes the need to invoke the activity on the fragment
     */
    fun setBottomNavigationBarVisibility(visible: Boolean) {
        val visibility = if (visible)
            View.VISIBLE
        else
            View.GONE
        bottomNavigationView.setVisibility(visibility)
        bottomNavigationViewFrame.setVisibility(visibility)
    }

    fun setNavigationDrawerLock(locked: Boolean) {
        val mode = if (locked)
            DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        else
            DrawerLayout.LOCK_MODE_UNLOCKED
        drawerLayout.setDrawerLockMode(mode)
    }

    val isNavigationDrawerLocked: Boolean
        get() = drawerLayout.getDrawerLockMode(navigationView) != DrawerLayout.LOCK_MODE_UNLOCKED

    @OptIn(markerClass = [UnstableApi::class])
    fun toggleNavigationDrawerLockOnOrientationChange(
        activity: AppCompatActivity
    ) {
        val orientation = activity.getResources().getConfiguration().orientation
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE

        if (getEnableDrawerOnPortrait()) {
            setNavigationDrawerLock(false)
            return
        }
        setNavigationDrawerLock(!isLandscape)
    }

    /*
   Auxiliar functions, could be moved somewhere else
    */
    @OptIn(markerClass = [UnstableApi::class])
    fun setSystemBarsVisibility(activity: AppCompatActivity, visibility: Boolean) {
        val insetsController: WindowInsetsControllerCompat?
        val window = activity.getWindow()
        val decorView = window.getDecorView()
        insetsController = WindowInsetsControllerCompat(window, decorView)

        if (visibility) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            insetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            )
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
            insetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            )
        }
    }

    companion object {
        @Contract(pure = true)
        private fun isTargetDestination(destination: NavDestination): Boolean {
            val destId = destination.id
            return destId == R.id.homeFragment || destId == R.id.libraryFragment || destId == R.id.downloadFragment || destId == R.id.albumCatalogueFragment || destId == R.id.artistCatalogueFragment || destId == R.id.genreCatalogueFragment || destId == R.id.playlistCatalogueFragment
        }
    }
}
