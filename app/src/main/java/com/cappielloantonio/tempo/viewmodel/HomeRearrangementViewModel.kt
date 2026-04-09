package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.model.HomeSector
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.util.Preferences.getHomeSectorList
import com.cappielloantonio.tempo.util.Preferences.setHomeSectorList
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

class HomeRearrangementViewModel(application: Application) : AndroidViewModel(application) {
    private var sectors: MutableList<HomeSector?>? = ArrayList<HomeSector?>()

    val homeSectorList: MutableList<HomeSector?>?
        get() {
            if (sectors != null && !sectors!!.isEmpty()) return sectors

            if (getHomeSectorList() != null && getHomeSectorList() != "null") {
                sectors = Gson().fromJson<MutableList<HomeSector?>?>(
                    getHomeSectorList(),
                    object :
                        TypeToken<MutableList<HomeSector?>?>() {
                    }.getType()
                )
            } else {
                sectors = fillStandardHomeSectorList()
            }

            return sectors
        }

    fun orderSectorLiveListAfterSwap(sectors: MutableList<HomeSector?>?) {
        this.sectors = sectors
    }

    fun saveHomeSectorList(sectors: MutableList<HomeSector?>?) {
        Preferences.setHomeSectorList(sectors?.filterNotNull())
    }

    fun resetHomeSectorList() {
        setHomeSectorList(null)
    }

    fun closeDialog() {
        sectors = null
    }

    private fun fillStandardHomeSectorList(): MutableList<HomeSector?> {
        val sectors: MutableList<HomeSector?> = ArrayList<HomeSector?>()

        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_DISCOVERY,
                getApplication<Application>()!!.getString(R.string.home_title_discovery),
                true,
                1
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_MADE_FOR_YOU,
                getApplication<Application>()!!.getString(R.string.home_title_made_for_you),
                true,
                2
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_BEST_OF,
                getApplication<Application>()!!.getString(R.string.home_title_best_of),
                true,
                3
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_RADIO_STATION,
                getApplication<Application>()!!.getString(R.string.home_title_radio_station),
                true,
                4
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_TOP_SONGS,
                getApplication<Application>()!!.getString(R.string.home_title_top_songs),
                true,
                5
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_STARRED_TRACKS,
                getApplication<Application>()!!.getString(R.string.home_title_starred_tracks),
                true,
                6
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_STARRED_ALBUMS,
                getApplication<Application>()!!.getString(R.string.home_title_starred_albums),
                true,
                7
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_STARRED_ARTISTS,
                getApplication<Application>()!!.getString(R.string.home_title_starred_artists),
                true,
                8
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_NEW_RELEASES,
                getApplication<Application>()!!.getString(R.string.home_title_new_releases),
                true,
                9
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_FLASHBACK,
                getApplication<Application>()!!.getString(R.string.home_title_flashback),
                true,
                10
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_MOST_PLAYED,
                getApplication<Application>()!!.getString(R.string.home_title_most_played),
                true,
                11
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_LAST_PLAYED,
                getApplication<Application>()!!.getString(R.string.home_title_last_played),
                true,
                12
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_RECENTLY_ADDED,
                getApplication<Application>()!!.getString(R.string.home_title_recently_added),
                true,
                13
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_PINNED_PLAYLISTS,
                getApplication<Application>()!!.getString(R.string.home_title_pinned_playlists),
                true,
                14
            )
        )
        sectors.add(
            HomeSector(
                Constants.HOME_SECTOR_SHARED,
                getApplication<Application>()!!.getString(R.string.home_title_shares),
                true,
                15
            )
        )

        return sectors
    }
}
