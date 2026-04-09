package com.cappielloantonio.tempo.ui.activity

import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.FrameLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.cappielloantonio.tempo.App.Companion.refreshSubsonicClient
import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.broadcast.receiver.ConnectivityStatusBroadcastReceiver
import com.cappielloantonio.tempo.databinding.ActivityMainBinding
import com.cappielloantonio.tempo.github.models.LatestRelease
import com.cappielloantonio.tempo.github.utils.UpdateUtil
import com.cappielloantonio.tempo.navigation.NavigationController
import com.cappielloantonio.tempo.navigation.NavigationHelper
import com.cappielloantonio.tempo.service.MediaManager.check
import com.cappielloantonio.tempo.service.MediaManager.hide
import com.cappielloantonio.tempo.service.MediaManager.playDownloadedMediaItem
import com.cappielloantonio.tempo.service.MediaManager.reset
import com.cappielloantonio.tempo.subsonic.models.OpenSubsonicExtension
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import com.cappielloantonio.tempo.ui.activity.base.BaseActivity
import com.cappielloantonio.tempo.ui.controller.BottomSheetController
import com.cappielloantonio.tempo.ui.controller.BottomSheetHelper
import com.cappielloantonio.tempo.ui.dialog.ConnectionAlertDialog
import com.cappielloantonio.tempo.ui.dialog.GithubTempoUpdateDialog
import com.cappielloantonio.tempo.ui.dialog.ServerUnreachableDialog
import com.cappielloantonio.tempo.ui.fragment.PlayerBottomSheetFragment
import com.cappielloantonio.tempo.util.AssetLinkNavigator
import com.cappielloantonio.tempo.util.AssetLinkUtil.AssetLink
import com.cappielloantonio.tempo.util.AssetLinkUtil.parse
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.Preferences.getHideBottomNavbarOnPortrait
import com.cappielloantonio.tempo.util.Preferences.getPassword
import com.cappielloantonio.tempo.util.Preferences.getSalt
import com.cappielloantonio.tempo.util.Preferences.getToken
import com.cappielloantonio.tempo.util.Preferences.isGithubUpdateEnabled
import com.cappielloantonio.tempo.util.Preferences.isInUseServerAddressLocal
import com.cappielloantonio.tempo.util.Preferences.isServerSwitchable
import com.cappielloantonio.tempo.util.Preferences.isWifiOnly
import com.cappielloantonio.tempo.util.Preferences.setClientCert
import com.cappielloantonio.tempo.util.Preferences.setDataSavingMode
import com.cappielloantonio.tempo.util.Preferences.setLocalAddress
import com.cappielloantonio.tempo.util.Preferences.setOpenSubsonic
import com.cappielloantonio.tempo.util.Preferences.setPassword
import com.cappielloantonio.tempo.util.Preferences.setPlaybackSpeed
import com.cappielloantonio.tempo.util.Preferences.setSalt
import com.cappielloantonio.tempo.util.Preferences.setServer
import com.cappielloantonio.tempo.util.Preferences.setServerId
import com.cappielloantonio.tempo.util.Preferences.setServerSwitchableTimer
import com.cappielloantonio.tempo.util.Preferences.setSkipSilenceMode
import com.cappielloantonio.tempo.util.Preferences.setStarredAlbumsSyncEnabled
import com.cappielloantonio.tempo.util.Preferences.setStarredSyncEnabled
import com.cappielloantonio.tempo.util.Preferences.setToken
import com.cappielloantonio.tempo.util.Preferences.setUser
import com.cappielloantonio.tempo.util.Preferences.showServerUnreachableDialog
import com.cappielloantonio.tempo.util.Preferences.showTempusUpdateDialog
import com.cappielloantonio.tempo.util.Preferences.switchInUseServerAddress
import com.cappielloantonio.tempo.viewmodel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationView
import com.google.common.util.concurrent.MoreExecutors
import java.util.Objects
import java.util.concurrent.ExecutionException

@UnstableApi
class MainActivity : BaseActivity() {
    @JvmField
    var binding: ActivityMainBinding? = null
    private var mainViewModel: MainViewModel? = null

    private val fragmentManager: FragmentManager? = null
    private val navHostFragment: NavHostFragment? = null
    private val bottomNavigationView: BottomNavigationView? = null
    private val bottomNavigationViewFrame: FrameLayout? = null
    private val drawerLayout: DrawerLayout? = null
    private val navigationView: NavigationView? = null
    @JvmField
    var navController: NavController? = null
    private var navigationController: NavigationController? = null
    private var bottomSheetController: BottomSheetController? = null
    var bottomSheetBehavior: BottomSheetBehavior<View?>? = null
    @JvmField
    var isLandscape: Boolean = false
    private var assetLinkNavigator: AssetLinkNavigator? = null
    private var pendingAssetLink: AssetLink? = null

    var connectivityStatusBroadcastReceiver: ConnectivityStatusBroadcastReceiver? = null
    private var pendingDownloadPlaybackIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)

        this.binding = ActivityMainBinding.inflate(getLayoutInflater())
        val view: View = binding!!.getRoot()
        setContentView(view)

        mainViewModel = ViewModelProvider(this).get<MainViewModel>(MainViewModel::class.java)
        assetLinkNavigator = AssetLinkNavigator(this)

        connectivityStatusBroadcastReceiver = ConnectivityStatusBroadcastReceiver(this)
        connectivityStatusReceiverManager(true)

        isLandscape =
            (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)

        init()
        checkConnectionType()
        this.openSubsonicExtensions
        checkTempoUpdate()

        maybeSchedulePlaybackIntent(getIntent())
    }

    override fun onStart() {
        super.onStart()
        pingServer()
        initService()
        consumePendingPlaybackIntent()
    }

    override fun onResume() {
        super.onResume()
        pingServer()
        toggleNavigationDrawerLockOnOrientationChange()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityStatusReceiverManager(false)
        this.binding = null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        maybeSchedulePlaybackIntent(intent)
        consumePendingPlaybackIntent()
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior!!.getState() == BottomSheetBehavior.STATE_EXPANDED) collapseBottomSheetDelayed()
        else super.onBackPressed()
    }

    fun init() {
        initBottomSheet()
        initNavigation()

        if (getPassword() != null || (getToken() != null && getSalt() != null)) {
            goFromLogin()
        } else {
            goToLogin()
        }

        toggleNavigationDrawerLockOnOrientationChange()
    }

    private fun initNavigation() {
        // We link the nav_graph.xml with our navigationController
        val navHostFragment = this
            .getSupportFragmentManager()
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        navController = Objects.requireNonNull<NavHostFragment?>(navHostFragment).navController

        /*
        navController is currently global since some legacy code still invokes it directly
        the MainActivity methods that use it must be converted to NavigationHelper methods
        */

        // Helper
        val navigationHelper =
            NavigationHelper(
                findViewById<BottomNavigationView>(R.id.bottom_navigation),
                findViewById<FrameLayout>(R.id.bottom_navigation_frame),
                findViewById<DrawerLayout>(R.id.drawer_layout),
                findViewById<NavigationView>(R.id.nav_view),
                navHostFragment!!
            )

        // Controller
        navigationController = NavigationController(navigationHelper)
        navigationController!!.syncWithBottomSheetBehavior(bottomSheetBehavior!!, navController!!)
    }

    private fun initBottomSheet() {
        val fragmentManager = getSupportFragmentManager()
        val bottomSheetView = findViewById<View>(R.id.player_bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)

        /*
        bottomSheetBehavior is currently global since some legacy code still invokes it directly
        the MainActivity methods that use it must be converted to BottomSheetHelper methods
        */

        // Helper
        val bottomSheetHelper =
            BottomSheetHelper(
                bottomSheetBehavior!!,
                bottomSheetView,
                fragmentManager
            )

        // Controller
        bottomSheetController = BottomSheetController(bottomSheetHelper)
        bottomSheetController!!.addCallback(bottomSheetCallback)
        bottomSheetController!!.replaceFragment(R.id.player_bottom_sheet)
        bottomSheetController!!.checkAfterStateChanged(mainViewModel!!)
    }

    fun setBottomSheetInPeek(isVisible: Boolean) {
        bottomSheetController!!.setStateInPeek(isVisible)
    }

    fun setBottomSheetVisibility(visibility: Boolean) {
        bottomSheetController!!.setVisibility(visibility)
    }

    fun collapseBottomSheetDelayed() {
        bottomSheetController!!.collapseDelayed()
    }

    fun expandBottomSheet() {
        bottomSheetController!!.expand()
    }

    fun setBottomSheetDraggableState(isDraggable: Boolean) {
        bottomSheetController!!.setDraggable(isDraggable)
    }

    private val bottomSheetCallback: BottomSheetCallback = object : BottomSheetCallback() {
        var navigationHeight: Int = 0

        override fun onStateChanged(view: View, state: Int) {
            val playerBottomSheetFragment =
                getSupportFragmentManager().findFragmentByTag("PlayerBottomSheet") as PlayerBottomSheetFragment?

            when (state) {
                BottomSheetBehavior.STATE_HIDDEN -> resetMusicSession() // I can't put the callback inside BottomSheetHelper because of this line
                BottomSheetBehavior.STATE_COLLAPSED -> if (playerBottomSheetFragment != null) playerBottomSheetFragment.goBackToFirstPage()
                BottomSheetBehavior.STATE_SETTLING, BottomSheetBehavior.STATE_EXPANDED, BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_HALF_EXPANDED -> {}
            }
        }

        override fun onSlide(view: View, slideOffset: Float) {
            animateBottomSheet(slideOffset)
            if (!isLandscape) {
                animateBottomNavigation(slideOffset, navigationHeight)
            }
        }
    }

    private fun animateBottomSheet(slideOffset: Float) {
        bottomSheetController!!.animate(slideOffset)
    }

    private fun animateBottomNavigation(slideOffset: Float, navigationHeight: Int) {
        var navigationHeight = navigationHeight
        if (slideOffset < 0) return

        if (navigationHeight == 0) {
            navigationHeight = binding!!.bottomNavigation.getHeight()
        }

        val slideY = navigationHeight - navigationHeight * (1 - slideOffset)

        binding!!.bottomNavigation.setTranslationY(slideY)
    }

    fun setBottomNavigationBarVisibility(visibility: Boolean) {
        navigationController!!.setNavbarVisibility(visibility)
    }

    fun toggleBottomNavigationBarVisibilityOnOrientationChange() {
        val displayDensity = getResources().getDisplayMetrics().density
        // Ignore orientation change, bottom navbar always hidden
        if (getHideBottomNavbarOnPortrait()) {
            navigationController!!.setNavbarVisibility(false)
            bottomSheetController!!.setPeekHeight(56, displayDensity)
            navigationController!!.setSystemBarsVisibility(this, !isLandscape)
            return
        }

        if (!isLandscape) {
            // Show app navbar + show system bars
            bottomSheetController!!.setPeekHeight(136, displayDensity)
            navigationController!!.setNavbarVisibility(true)
            navigationController!!.setSystemBarsVisibility(this, true)
        } else {
            // Hide app navbar + hide system bars
            bottomSheetController!!.setPeekHeight(56, displayDensity)
            navigationController!!.setNavbarVisibility(false)
            navigationController!!.setSystemBarsVisibility(this, false)
        }
    }

    fun setNavigationDrawerLock(locked: Boolean) {
        navigationController!!.setDrawerLock(locked)
    }

    val isNavigationDrawerLocked: Boolean
        get() = navigationController!!.isNavigationDrawerLocked

    fun toggleNavigationDrawerLockOnOrientationChange() {
        navigationController!!.toggleDrawerLockOnOrientation(this)
    }

    fun setSystemBarsVisibility(visibility: Boolean) {
        navigationController!!.setSystemBarsVisibility(this, visibility)
    }

    /*
    There are only 4 init functions that must exist up to here
    1. init()
    2. initNavigation()
    3. initBottomSheet()
    4. bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() { ... }
     */
    private fun initService() {
        val future = getMediaBrowserListenableFuture()
        check(future)

        future?.addListener(Runnable {
            try {
                future.get()?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying && bottomSheetBehavior!!.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                            setBottomSheetInPeek(true)
                        }
                    }
                })
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun goToLogin() {
        bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
        setBottomNavigationBarVisibility(false)
        setBottomSheetVisibility(false)

        if (Objects.requireNonNull<NavDestination?>(navController!!.currentDestination).id == R.id.landingFragment) {
            navController!!.navigate(R.id.action_landingFragment_to_loginFragment)
        } else if (Objects.requireNonNull<NavDestination?>(navController!!.currentDestination).id == R.id.settingsFragment) {
            navController!!.navigate(R.id.action_settingsFragment_to_loginFragment)
        } else if (Objects.requireNonNull<NavDestination?>(navController!!.currentDestination).id == R.id.homeFragment) {
            navController!!.navigate(R.id.action_homeFragment_to_loginFragment)
        }
    }

    private fun goToHome() {
        setBottomNavigationBarVisibility(true)

        if (Objects.requireNonNull<NavDestination?>(navController!!.currentDestination).id == R.id.landingFragment) {
            navController!!.navigate(R.id.action_landingFragment_to_homeFragment)
        } else if (Objects.requireNonNull<NavDestination?>(navController!!.currentDestination).id == R.id.loginFragment) {
            navController!!.navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    fun goFromLogin() {
        setBottomSheetInPeek(mainViewModel!!.isQueueLoaded)
        goToHome()
        consumePendingAssetLink()
    }

    @JvmOverloads
    fun openAssetLink(assetLink: AssetLink, collapsePlayer: Boolean = true) {
        if (!this.isUserAuthenticated) {
            pendingAssetLink = assetLink
            return
        }
        if (collapsePlayer) {
            setBottomSheetInPeek(true)
        }
        if (assetLinkNavigator != null) {
            assetLinkNavigator!!.open(assetLink)
        }
    }

    fun quit() {
        resetUserSession()
        resetMusicSession()
        resetViewModel()
        goToLogin()
    }

    private fun resetUserSession() {
        setServerId(null)
        setSalt(null)
        setToken(null)
        setPassword(null)
        setServer(null)
        setLocalAddress(null)
        setUser(null)
        setClientCert(null)

        // TODO Enter all settings to be reset
        setOpenSubsonic(false)
        setPlaybackSpeed(1.0f)
        setSkipSilenceMode(false)
        setDataSavingMode(false)
        setStarredSyncEnabled(false)
        setStarredAlbumsSyncEnabled(false)
    }

    private fun resetMusicSession() {
        reset(getMediaBrowserListenableFuture())
    }

    private fun hideMusicSession() {
        hide(getMediaBrowserListenableFuture())
    }

    private fun resetViewModel() {
        viewModelStore.clear()
    }

    // CONNECTION
    private fun connectivityStatusReceiverManager(isActive: Boolean) {
        if (isActive) {
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiver(connectivityStatusBroadcastReceiver, filter)
        } else {
            unregisterReceiver(connectivityStatusBroadcastReceiver)
        }
    }

    private fun pingServer() {
        if (getToken() == null && getPassword() == null) return

        if (isInUseServerAddressLocal()) {
            mainViewModel!!.ping().observe(this, Observer { subsonicResponse: SubsonicResponse? ->
                if (subsonicResponse == null) {
                    setServerSwitchableTimer()
                    switchInUseServerAddress()
                    refreshSubsonicClient()
                    pingServer()
                    resetView()
                } else {
                    setOpenSubsonic(subsonicResponse.openSubsonic != null && subsonicResponse.openSubsonic == true)
                }
            })
        } else {
            if (isServerSwitchable()) {
                setServerSwitchableTimer()
                switchInUseServerAddress()
                refreshSubsonicClient()
                pingServer()
                resetView()
            } else {
                mainViewModel!!.ping()
                    .observe(this, Observer { subsonicResponse: SubsonicResponse? ->
                        if (subsonicResponse == null) {
                            if (showServerUnreachableDialog()) {
                                val dialog = ServerUnreachableDialog()
                                dialog.show(getSupportFragmentManager(), null)
                            }
                        } else {
                            setOpenSubsonic(subsonicResponse.openSubsonic != null && subsonicResponse.openSubsonic == true)
                        }
                    })
            }
        }
    }

    private fun resetView() {
        resetViewModel()
        val id = Objects.requireNonNull<NavDestination?>(navController!!.currentDestination).id
        navController!!.popBackStack(id, true)
        navController!!.navigate(id)
    }

    private val openSubsonicExtensions: Unit
        get() {
            if (getToken() != null || getPassword() != null) {
                mainViewModel!!.openSubsonicExtensions.observe(
                    this,
                    Observer { openSubsonicExtensions: MutableList<OpenSubsonicExtension?>? ->
                        if (openSubsonicExtensions != null) {
                            Preferences.setOpenSubsonicExtensions(
                                openSubsonicExtensions.filterNotNull()
                            )
                        }
                    })
            }
        }

    private fun checkTempoUpdate() {
        if (isGithubUpdateEnabled() && showTempusUpdateDialog()) {
            mainViewModel!!.checkTempoUpdate()
                .observe(this, Observer { latestRelease: LatestRelease? ->
                    if (latestRelease != null && UpdateUtil.showUpdateDialog(latestRelease)) {
                        val dialog = GithubTempoUpdateDialog(latestRelease)
                        dialog.show(getSupportFragmentManager(), null)
                    }
                })
        }
    }

    private fun checkConnectionType() {
        if (isWifiOnly()) {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.getActiveNetworkInfo()

            if (networkInfo != null && networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
                val dialog = ConnectionAlertDialog()
                dialog.show(getSupportFragmentManager(), null)
            }
        }
    }

    private fun maybeSchedulePlaybackIntent(intent: Intent?) {
        if (intent == null) return
        if (Constants.ACTION_PLAY_EXTERNAL_DOWNLOAD == intent.getAction()
            || intent.hasExtra(Constants.EXTRA_DOWNLOAD_URI)
        ) {
            pendingDownloadPlaybackIntent = Intent(intent)
        }
        handleAssetLinkIntent(intent)
    }

    private fun consumePendingPlaybackIntent() {
        if (pendingDownloadPlaybackIntent == null) return
        val intent = pendingDownloadPlaybackIntent
        pendingDownloadPlaybackIntent = null
        playDownloadedMedia(intent!!)
    }

    private fun handleAssetLinkIntent(intent: Intent) {
        val assetLink = parse(intent)
        if (assetLink == null) {
            return
        }
        if (!this.isUserAuthenticated) {
            pendingAssetLink = assetLink
            intent.setData(null)
            return
        }
        if (assetLinkNavigator != null) {
            assetLinkNavigator!!.open(assetLink)
        }
        intent.setData(null)
    }

    private val isUserAuthenticated: Boolean
        get() = getPassword() != null
                || (getToken() != null && getSalt() != null)

    private fun consumePendingAssetLink() {
        if (pendingAssetLink == null || assetLinkNavigator == null) {
            return
        }
        assetLinkNavigator!!.open(pendingAssetLink)
        pendingAssetLink = null
    }

    private fun playDownloadedMedia(intent: Intent) {
        val uriString = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_URI)
        if (TextUtils.isEmpty(uriString)) {
            return
        }

        val uri = Uri.parse(uriString)
        var mediaId = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_MEDIA_ID)
        if (TextUtils.isEmpty(mediaId)) {
            mediaId = uri.toString()
        }

        val title = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_TITLE)
        val artist = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_ARTIST)
        val album = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_ALBUM)
        val duration = intent.getIntExtra(Constants.EXTRA_DOWNLOAD_DURATION, 0)

        val extras = Bundle()
        extras.putString("id", mediaId)
        extras.putString("title", title)
        extras.putString("artist", artist)
        extras.putString("album", album)
        extras.putString("uri", uri.toString())
        extras.putString("type", Constants.MEDIA_TYPE_MUSIC)
        extras.putInt("duration", duration)

        val metadataBuilder = MediaMetadata.Builder()
            .setExtras(extras)
            .setIsBrowsable(false)
            .setIsPlayable(true)

        if (!TextUtils.isEmpty(title)) metadataBuilder.setTitle(title)
        if (!TextUtils.isEmpty(artist)) metadataBuilder.setArtist(artist)
        if (!TextUtils.isEmpty(album)) metadataBuilder.setAlbumTitle(album)

        val mediaItem = MediaItem.Builder()
            .setMediaId(mediaId!!)
            .setMediaMetadata(metadataBuilder.build())
            .setUri(uri)
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setRequestMetadata(
                RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .setExtras(extras)
                    .build()
            )
            .build()

        playDownloadedMediaItem(getMediaBrowserListenableFuture(), mediaItem)
    }

    companion object {
        private const val TAG = "MainActivityLogs"
    }
}
