package com.cappielloantonio.tempo.ui.fragment

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.os.IBinder
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreference.OnBindEditTextListener
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.helper.ThemeHelper.applyTheme
import com.cappielloantonio.tempo.interfaces.DialogClickCallback
import com.cappielloantonio.tempo.interfaces.ScanCallback
import com.cappielloantonio.tempo.service.BaseMediaService
import com.cappielloantonio.tempo.service.BaseMediaService.LocalBinder
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.DeleteDownloadStorageDialog
import com.cappielloantonio.tempo.ui.dialog.DownloadStorageDialog
import com.cappielloantonio.tempo.ui.dialog.StarredAlbumSyncDialog
import com.cappielloantonio.tempo.ui.dialog.StarredArtistSyncDialog
import com.cappielloantonio.tempo.ui.dialog.StarredSyncDialog
import com.cappielloantonio.tempo.ui.dialog.StreamingCacheStorageDialog
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader.refreshCache
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.getDownloadStoragePreference
import com.cappielloantonio.tempo.util.Preferences.getStreamingCacheStoragePreference
import com.cappielloantonio.tempo.util.Preferences.isAutoDownloadLyricsEnabled
import com.cappielloantonio.tempo.util.Preferences.setAutoDownloadLyricsEnabled
import com.cappielloantonio.tempo.util.Preferences.setDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.setDownloadStoragePreference
import com.cappielloantonio.tempo.util.Preferences.setShuffleInsteadOfHeart
import com.cappielloantonio.tempo.util.Preferences.showShuffleInsteadOfHeart
import com.cappielloantonio.tempo.util.UIUtil.getLangPreferenceDropdownEntries
import com.cappielloantonio.tempo.viewmodel.SettingViewModel
import java.util.Locale

@OptIn(markerClass = [UnstableApi::class])
class SettingsContainerFragment : PreferenceFragmentCompat() {
    private var activity: MainActivity? = null

    private var settingViewModel: SettingViewModel? = null

    private var directoryPickerLauncher: ActivityResultLauncher<Intent?>? = null

    private var mediaServiceBinder: LocalBinder? = null
    private var isServiceBound = false
    private var equalizerResultLauncher: ActivityResultLauncher<Intent?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        equalizerResultLauncher = registerForActivityResult<Intent?, ActivityResult?>(
            StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? -> }
        )

        // GitHub update settings always available

        directoryPickerLauncher = registerForActivityResult<Intent?, ActivityResult?>(
            StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? ->
                if (result!!.getResultCode() == Activity.RESULT_OK) {
                    val data = result.getData()
                    if (data != null) {
                        val uri = data.getData()
                        if (uri != null) {
                            requireContext().getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )

                            setDownloadDirectoryUri(uri.toString())
                            refreshCache()
                            Toast.makeText(
                                requireContext(),
                                R.string.settings_download_folder_set,
                                Toast.LENGTH_SHORT
                            ).show()
                            checkDownloadDirectory()
                        }
                    }
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        val view = super.onCreateView(inflater, container, savedInstanceState)
        settingViewModel =
            ViewModelProvider(requireActivity()).get<SettingViewModel>(SettingViewModel::class.java)

        getListView().setPadding(
            0,
            0,
            0,
            getResources().getDimension(R.dimen.global_padding_bottom).toInt()
        )

        return view
    }

    override fun onStart() {
        super.onStart()
        activity!!.setBottomNavigationBarVisibility(false)
        activity!!.setBottomSheetVisibility(false)
    }

    override fun onResume() {
        super.onResume()

        checkSystemEqualizer()
        checkCacheStorage()
        checkStorage()
        checkDownloadDirectory()

        setStreamingCacheSize()
        setAppLanguage()
        setVersion()
        setNetorkPingTimeoutBase()

        actionLogout()
        actionScan()
        actionSyncStarredAlbums()
        actionSyncStarredTracks()
        actionSyncStarredArtists()
        actionChangeStreamingCacheStorage()
        actionChangeDownloadStorage()
        actionSetDownloadDirectory()
        actionDeleteDownloadStorage()
        actionKeepScreenOn()
        actionAutoDownloadLyrics()
        actionMiniPlayerHeart()

        bindMediaService()
        actionAppEqualizer()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.global_preferences, rootKey)
        val themePreference = findPreference<ListPreference?>(Preferences.THEME)
        if (themePreference != null) {
            themePreference.setOnPreferenceChangeListener(
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    val themeOption = newValue as String
                    applyTheme(themeOption)
                    true
                })
        }
    }

    private fun checkSystemEqualizer() {
        val equalizer = findPreference<Preference?>("system_equalizer")

        if (equalizer == null) return

        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)

        if ((intent.resolveActivity(requireActivity().getPackageManager()) != null)) {
            equalizer.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                equalizerResultLauncher!!.launch(intent)
                true
            })
        } else {
            equalizer.setVisible(false)
        }
    }

    private fun checkCacheStorage() {
        val storage = findPreference<Preference?>("streaming_cache_storage")

        if (storage == null) return

        try {
            if (requireContext().getExternalFilesDirs(null)[1] == null) {
                storage.setVisible(false)
            } else {
                storage.setSummary(if (getStreamingCacheStoragePreference() == 0) R.string.download_storage_internal_dialog_negative_button else R.string.download_storage_external_dialog_positive_button)
            }
        } catch (exception: Exception) {
            storage.setVisible(false)
        }
    }

    private fun checkStorage() {
        val storage = findPreference<Preference?>("download_storage")

        if (storage == null) return

        try {
            if (requireContext().getExternalFilesDirs(null)[1] == null) {
                storage.setVisible(false)
            } else {
                val pref = getDownloadStoragePreference()
                if (pref == 0) {
                    storage.setSummary(R.string.download_storage_internal_dialog_negative_button)
                } else if (pref == 1) {
                    storage.setSummary(R.string.download_storage_external_dialog_positive_button)
                } else {
                    storage.setSummary(R.string.download_storage_directory_dialog_neutral_button)
                }
            }
        } catch (exception: Exception) {
            storage.setVisible(false)
        }
    }

    private fun checkDownloadDirectory() {
        val storage = findPreference<Preference?>("download_storage")
        val directory = findPreference<Preference?>("set_download_directory")

        if (directory == null) return

        val current = getDownloadDirectoryUri()
        if (current != null) {
            if (storage != null) storage.setVisible(false)
            directory.setVisible(true)
            directory.setIcon(R.drawable.ic_close)
            directory.setTitle(R.string.settings_clear_download_folder)
            directory.setSummary(current)
        } else {
            if (storage != null) storage.setVisible(true)
            if (getDownloadStoragePreference() == 2) {
                directory.setVisible(true)
                directory.setIcon(R.drawable.ic_folder)
                directory.setTitle(R.string.settings_set_download_folder)
                directory.setSummary(R.string.settings_choose_download_folder)
            } else {
                directory.setVisible(false)
            }
        }
    }

    private fun setNetorkPingTimeoutBase() {
        val networkPingTimeoutBase =
            findPreference<EditTextPreference?>("network_ping_timeout_base")

        if (networkPingTimeoutBase != null) {
            networkPingTimeoutBase.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance())
            networkPingTimeoutBase.setOnBindEditTextListener(OnBindEditTextListener { editText: EditText? ->
                editText!!.setInputType(InputType.TYPE_CLASS_NUMBER)
                editText.filters = arrayOf(InputFilter { source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int ->
                    for (i in start..<end) {
                        if (!Character.isDigit(source!!.get(i))) {
                            return@InputFilter ""
                        }
                    }
                    null
                })
            })

            networkPingTimeoutBase.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                val input = newValue as String?
                input != null && !input.isEmpty()
            })
        }
    }

    private fun setStreamingCacheSize() {
        val streamingCachePreference = findPreference<ListPreference?>("streaming_cache_size")

        if (streamingCachePreference != null) {
            streamingCachePreference.setSummaryProvider(object : SummaryProvider<ListPreference?> {
                override fun provideSummary(preference: ListPreference): CharSequence? {
                    val entry = preference.getEntry()

                    if (entry == null) return null

                    val currentSizeMb =
                        DownloadUtil.getStreamingCacheSize(requireActivity()) / (1024 * 1024)

                    return getString(
                        R.string.settings_summary_streaming_cache_size,
                        entry,
                        currentSizeMb.toString()
                    )
                }
            })
        }
    }

    private fun setAppLanguage() {
        val localePref = findPreference<Preference?>("language") as ListPreference?

        val locales = getLangPreferenceDropdownEntries(requireContext())

        val entries = locales.keys.toTypedArray<CharSequence?>()
        val entryValues = locales.values.toTypedArray<CharSequence?>()

        localePref!!.setEntries(entries)
        localePref.setEntryValues(entryValues)

        val value = localePref.getValue()
        if ("default" == value) {
            localePref.setSummary(requireContext().getString(R.string.settings_system_language))
        } else {
            localePref.setSummary(Locale.forLanguageTag(value).getDisplayName())
        }

        localePref.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
            if ("default" == newValue) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                preference!!.setSummary(requireContext().getString(R.string.settings_system_language))
            } else {
                val languageTag = newValue as String
                val appLocale = LocaleListCompat.forLanguageTags(languageTag)
                AppCompatDelegate.setApplicationLocales(appLocale)
                preference!!.setSummary(Locale.forLanguageTag(languageTag).getDisplayName())
            }
            true
        })
    }

    private fun setVersion() {
        findPreference<Preference?>("version")!!.setSummary(BuildConfig.VERSION_NAME)
    }

    private fun actionLogout() {
        findPreference<Preference?>("logout")!!.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
            activity!!.quit()
            true
        })
    }

    private fun actionScan() {
        findPreference<Preference?>("scan_library")!!.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
            settingViewModel!!.launchScan(object : ScanCallback {
                override fun onError(exception: Exception?) {
                    findPreference<Preference?>("scan_library")!!.setSummary(exception?.message)
                }

                override fun onSuccess(isScanning: Boolean, count: Long) {
                    findPreference<Preference?>("scan_library")!!.setSummary(
                        getString(
                            R.string.settings_scan_result,
                            count
                        )
                    )
                    if (isScanning) this@SettingsContainerFragment.scanStatus
                }
            })
            true
        })
    }

    private fun actionSyncStarredTracks() {
        findPreference<Preference?>("sync_starred_tracks_for_offline_use")!!.setOnPreferenceChangeListener(
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                if (newValue is Boolean) {
                    if (newValue) {
                        val dialog = StarredSyncDialog(Runnable {
                            (preference as SwitchPreference).setChecked(false)
                        })
                        dialog.show(activity!!.getSupportFragmentManager(), null)
                    }
                }
                true
            })
    }

    private fun actionSyncStarredAlbums() {
        findPreference<Preference?>("sync_starred_albums_for_offline_use")!!.setOnPreferenceChangeListener(
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                if (newValue is Boolean) {
                    if (newValue) {
                        val dialog = StarredAlbumSyncDialog(Runnable {
                            (preference as SwitchPreference).setChecked(false)
                        })
                        dialog.show(activity!!.getSupportFragmentManager(), null)
                    }
                }
                true
            })
    }

    private fun actionSyncStarredArtists() {
        findPreference<Preference?>("sync_starred_artists_for_offline_use")!!.setOnPreferenceChangeListener(
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                if (newValue is Boolean) {
                    if (newValue) {
                        val dialog = StarredArtistSyncDialog(Runnable {
                            (preference as SwitchPreference).setChecked(false)
                        })
                        dialog.show(activity!!.getSupportFragmentManager(), null)
                    }
                }
                true
            })
    }

    private fun actionChangeStreamingCacheStorage() {
        findPreference<Preference?>("streaming_cache_storage")!!.setOnPreferenceClickListener(
            Preference.OnPreferenceClickListener { preference: Preference? ->
                val dialog = StreamingCacheStorageDialog(object : DialogClickCallback {
                    override fun onPositiveClick() {
                        findPreference<Preference?>("streaming_cache_storage")!!.setSummary(R.string.streaming_cache_storage_external_dialog_positive_button)
                    }

                    override fun onNegativeClick() {
                        findPreference<Preference?>("streaming_cache_storage")!!.setSummary(R.string.streaming_cache_storage_internal_dialog_negative_button)
                    }
                })
                dialog.show(activity!!.getSupportFragmentManager(), null)
                true
            })
    }

    private fun actionChangeDownloadStorage() {
        findPreference<Preference?>("download_storage")!!.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
            val dialog = DownloadStorageDialog(object : DialogClickCallback {
                override fun onPositiveClick() {
                    findPreference<Preference?>("download_storage")!!.setSummary(R.string.download_storage_external_dialog_positive_button)
                    checkDownloadDirectory()
                }

                override fun onNegativeClick() {
                    findPreference<Preference?>("download_storage")!!.setSummary(R.string.download_storage_internal_dialog_negative_button)
                    checkDownloadDirectory()
                }

                override fun onNeutralClick() {
                    findPreference<Preference?>("download_storage")!!.setSummary(R.string.download_storage_directory_dialog_neutral_button)
                    checkDownloadDirectory()
                }
            })
            dialog.show(activity!!.getSupportFragmentManager(), null)
            true
        })
    }

    private fun actionSetDownloadDirectory() {
        val pref = findPreference<Preference?>("set_download_directory")
        if (pref != null) {
            pref.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                val current = getDownloadDirectoryUri()
                if (current != null) {
                    setDownloadDirectoryUri(null)
                    setDownloadStoragePreference(0)
                    refreshCache()
                    Toast.makeText(
                        requireContext(),
                        R.string.settings_download_folder_cleared,
                        Toast.LENGTH_SHORT
                    ).show()
                    checkStorage()
                    checkDownloadDirectory()
                } else {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addFlags(
                        (Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    )
                    directoryPickerLauncher!!.launch(intent)
                }
                true
            })
        }
    }

    private fun actionDeleteDownloadStorage() {
        findPreference<Preference?>("delete_download_storage")!!.setOnPreferenceClickListener(
            Preference.OnPreferenceClickListener { preference: Preference? ->
                val dialog = DeleteDownloadStorageDialog()
                dialog.show(activity!!.getSupportFragmentManager(), null)
                true
            })
    }

    private fun actionMiniPlayerHeart() {
        val preference = findPreference<SwitchPreference?>("mini_shuffle_button_visibility")
        if (preference == null) {
            return
        }

        preference.setChecked(showShuffleInsteadOfHeart())
        preference.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { pref: Preference?, newValue: Any? ->
            if (newValue is Boolean) {
                setShuffleInsteadOfHeart(newValue)
            }
            true
        })
    }

    private fun actionAutoDownloadLyrics() {
        val preference = findPreference<SwitchPreference?>("auto_download_lyrics")
        if (preference == null) {
            return
        }

        preference.setChecked(isAutoDownloadLyricsEnabled())
        preference.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { pref: Preference?, newValue: Any? ->
            if (newValue is Boolean) {
                setAutoDownloadLyricsEnabled(newValue)
            }
            true
        })
    }

    private val scanStatus: Unit
        get() {
            settingViewModel!!.getScanStatus(object :
                ScanCallback {
                override fun onError(exception: Exception?) {
                    findPreference<Preference?>("scan_library")!!.setSummary(
                        exception?.message
                    )
                }

                override fun onSuccess(isScanning: Boolean, count: Long) {
                    findPreference<Preference?>("scan_library")!!.setSummary(
                        getString(R.string.settings_scan_result, count)
                    )
                    if (isScanning) settingViewModel!!.getScanStatus(this)
                }
            })
        }

    private fun actionKeepScreenOn() {
        findPreference<Preference?>("always_on_display")!!.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
            if (newValue is Boolean) {
                if (newValue) {
                    activity!!.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity!!.getWindow()
                        .clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            true
        })
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mediaServiceBinder = service as LocalBinder?
            isServiceBound = true
            checkEqualizerBands()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaServiceBinder = null
            isServiceBound = false
        }
    }

    private fun bindMediaService() {
        val intent = Intent(requireActivity(), MediaService::class.java)
        intent.setAction(BaseMediaService.ACTION_BIND_EQUALIZER)
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        isServiceBound = true
    }

    private fun checkEqualizerBands() {
        if (mediaServiceBinder != null) {
            val eqManager = mediaServiceBinder!!.getEqualizerManager()
            val numBands = eqManager.getNumberOfBands()
            val appEqualizer = findPreference<Preference?>("app_equalizer")
            if (appEqualizer != null) {
                appEqualizer.setVisible(numBands > 0)
            }
        }
    }

    private fun actionAppEqualizer() {
        val appEqualizer = findPreference<Preference?>("app_equalizer")
        if (appEqualizer != null) {
            appEqualizer.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                val navController = NavHostFragment.findNavController(this)
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.equalizerFragment, true)
                    .build()
                activity!!.setBottomNavigationBarVisibility(true)
                activity!!.setBottomSheetVisibility(true)
                navController.navigate(R.id.equalizerFragment, null, navOptions)
                true
            })
        }
    }

    override fun onPause() {
        super.onPause()
        if (isServiceBound) {
            requireActivity().unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }
}
