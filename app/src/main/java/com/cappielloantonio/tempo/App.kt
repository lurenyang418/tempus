package com.cappielloantonio.tempo

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.cappielloantonio.tempo.github.Github
import com.cappielloantonio.tempo.helper.ThemeHelper
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.SubsonicPreferences
import com.cappielloantonio.tempo.util.ClientCertManager
import com.cappielloantonio.tempo.util.Preferences.getInUseServerAddress
import com.cappielloantonio.tempo.util.Preferences.getPassword
import com.cappielloantonio.tempo.util.Preferences.getSalt
import com.cappielloantonio.tempo.util.Preferences.getServer
import com.cappielloantonio.tempo.util.Preferences.getToken
import com.cappielloantonio.tempo.util.Preferences.getUser
import com.cappielloantonio.tempo.util.Preferences.isLowScurity
import com.cappielloantonio.tempo.util.Preferences.setPassword
import com.cappielloantonio.tempo.util.Preferences.setSalt
import com.cappielloantonio.tempo.util.Preferences.setToken

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        val themePref: String = sharedPreferences.getString(
            com.cappielloantonio.tempo.util.Preferences.THEME,
            ThemeHelper.DEFAULT_MODE
        )!!
        ThemeHelper.applyTheme(themePref)

        instance = App()
        context = getApplicationContext()
        Companion.preferences = PreferenceManager.getDefaultSharedPreferences(context!!)

        ClientCertManager.setupSslSocketFactory(context!!)
    }

    val preferences: SharedPreferences?
        get() {
            if (Companion.preferences == null) {
                Companion.preferences =
                    PreferenceManager.getDefaultSharedPreferences(context!!)
            }

            return Companion.preferences
        }

    companion object {
        private var instance: App? = null
        private var context: Context? = null
        private var subsonic: Subsonic? = null
        private var github: Github? = null
        @JvmField
        var preferences: SharedPreferences? = null

        @JvmStatic
        fun getInstance(): App {
            if (instance == null) {
                instance = App()
            }

            return instance!!
        }

        @JvmStatic
        fun getContext(): Context? {
            if (context == null) {
                context = getInstance()
            }

            return context
        }

        @JvmStatic
        fun getSubsonicClientInstance(override: Boolean): Subsonic {
            if (subsonic == null || override) {
                subsonic = subsonicClient
            }
            return subsonic!!
        }

        @JvmStatic
        fun getSubsonicPublicClientInstance(override: Boolean): Subsonic {
            /*
                   If I do the shortcut that the IDE suggests:
                       SubsonicPreferences preferences = getSubsonicPreferences1();
                   During the chain of calls it will run the following:
                       String server = Preferences.getInUseServerAddress();
                   Which could return Local URL, causing issues like generating public shares with Local URL
           
                   To prevent this I just replicated the entire chain of functions here,
                   if you need a call to Subsonic using the Server (Public) URL use this function.
                    */

            val server = getServer()
            val username = getUser()
            val password = getPassword()
            val token = getToken()
            val salt = getSalt()
            val isLowSecurity = isLowScurity()

            val preferences = SubsonicPreferences()
            preferences.serverUrl = server
            preferences.username = username
            preferences.setAuthentication(password, token, salt, isLowSecurity)

            if (subsonic == null || override) {
                if (preferences.authentication != null) {
                    if (preferences.authentication!!.password != null) setPassword(
                        preferences.authentication!!.password
                    )
                    if (preferences.authentication!!
                            .token != null
                    ) setToken(preferences.authentication!!.token)
                    if (preferences.authentication!!
                            .salt != null
                    ) setSalt(preferences.authentication!!.salt)
                }
            }

            return Subsonic(preferences)
        }

        @JvmStatic
        val githubClientInstance: Github
            get() {
                if (github == null) {
                    github = Github()
                }
                return github!!
            }

        @JvmStatic
        fun refreshSubsonicClient() {
            subsonic = subsonicClient
        }

        private val subsonicClient: Subsonic
            get() {
                val preferences: SubsonicPreferences = subsonicPreferences

                if (preferences.authentication != null) {
                    if (preferences.authentication!!
                            .password != null
                    ) setPassword(
                        preferences.authentication!!.password
                    )
                    if (preferences.authentication!!
                            .token != null
                    ) setToken(
                        preferences.authentication!!.token
                    )
                    if (preferences.authentication!!
                            .salt != null
                    ) setSalt(
                        preferences.authentication!!.salt
                    )
                }

                return Subsonic(preferences)
            }

        private val subsonicPreferences: SubsonicPreferences
            get() {
                val server =
                    getInUseServerAddress()
                val username = getUser()
                val password =
                    getPassword()
                val token = getToken()
                val salt = getSalt()
                val isLowSecurity =
                    isLowScurity()

                val preferences = SubsonicPreferences()
                preferences.serverUrl = server
                preferences.username = username
                preferences.setAuthentication(password, token, salt, isLowSecurity)

                return preferences
            }
    }
}
