package com.cappielloantonio.tempo.util

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog.SongBottomSheetDialog
import com.cappielloantonio.tempo.util.AssetLinkUtil.AssetLink
import com.cappielloantonio.tempo.viewmodel.SongBottomSheetViewModel

class AssetLinkNavigator(private val activity: MainActivity) {
    private val songRepository = SongRepository()
    private val albumRepository = AlbumRepository()
    private val artistRepository = ArtistRepository()
    private val playlistRepository = PlaylistRepository()

    fun open(assetLink: AssetLink?) {
        if (assetLink == null) {
            return
        }
        when (assetLink.type) {
            AssetLinkUtil.TYPE_SONG -> openSong(assetLink.id)
            AssetLinkUtil.TYPE_ALBUM -> openAlbum(assetLink.id)
            AssetLinkUtil.TYPE_ARTIST -> openArtist(assetLink.id)
            AssetLinkUtil.TYPE_PLAYLIST -> openPlaylist(assetLink.id)
            AssetLinkUtil.TYPE_GENRE -> openGenre(assetLink.id)
            AssetLinkUtil.TYPE_YEAR -> openYear(assetLink.id)
            else -> Toast.makeText(
                activity,
                R.string.asset_link_error_unsupported,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openSong(id: String) {
        val liveData = songRepository.getSong(id)
        val observer: Observer<Child?> = object : Observer<Child?> {
            override fun onChanged(value: Child?) {
                val child = value
                liveData.removeObserver(this)
                if (child == null) {
                    Toast.makeText(activity, R.string.asset_link_error_song, Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                val viewModel = ViewModelProvider(activity).get<SongBottomSheetViewModel>(
                    SongBottomSheetViewModel::class.java
                )
                viewModel.setSong(child)
                val dialog = SongBottomSheetDialog()
                val args = Bundle()
                args.putParcelable(Constants.TRACK_OBJECT, child)
                dialog.setArguments(args)
                dialog.show(activity.getSupportFragmentManager(), null)
            }
        }
        liveData.observe(activity, observer)
    }

    private fun openAlbum(id: String) {
        val liveData = albumRepository.getAlbum(id)
        val observer: Observer<AlbumID3?> = object : Observer<AlbumID3?> {
            override fun onChanged(value: AlbumID3?) {
                val album = value
                liveData.removeObserver(this)
                if (album == null) {
                    Toast.makeText(activity, R.string.asset_link_error_album, Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                val args = Bundle()
                args.putParcelable(Constants.ALBUM_OBJECT, album)
                navigateSafely(R.id.albumPageFragment, args)
            }
        }
        liveData.observe(activity, observer)
    }

    private fun openArtist(id: String) {
        val liveData = artistRepository.getArtist(id)
        val observer: Observer<ArtistID3?> = object : Observer<ArtistID3?> {
            override fun onChanged(value: ArtistID3?) {
                val artist = value
                liveData.removeObserver(this)
                if (artist == null) {
                    Toast.makeText(activity, R.string.asset_link_error_artist, Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                val args = Bundle()
                args.putParcelable(Constants.ARTIST_OBJECT, artist)
                navigateSafely(R.id.artistPageFragment, args)
            }
        }
        liveData.observe(activity, observer)
    }

    private fun openPlaylist(id: String) {
        val liveData = playlistRepository.getPlaylist(id)
        val observer: Observer<Playlist?> = object : Observer<Playlist?> {
            override fun onChanged(value: Playlist?) {
                val playlist = value
                liveData.removeObserver(this)
                if (playlist == null) {
                    Toast.makeText(activity, R.string.asset_link_error_playlist, Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                val args = Bundle()
                args.putParcelable(Constants.PLAYLIST_OBJECT, playlist)
                navigateSafely(R.id.playlistPageFragment, args)
            }
        }
        liveData.observe(activity, observer)
    }

    private fun openGenre(genreName: String) {
        val trimmed = genreName.trim { it <= ' ' }
        if (trimmed.isEmpty()) {
            Toast.makeText(activity, R.string.asset_link_error_unsupported, Toast.LENGTH_SHORT)
                .show()
            return
        }

        val genre = Genre()
        genre.genre = trimmed
        genre.songCount = 0
        genre.albumCount = 0
        val args = Bundle()
        args.putParcelable(Constants.GENRE_OBJECT, genre)
        args.putString(Constants.MEDIA_BY_GENRE, Constants.MEDIA_BY_GENRE)
        navigateSafely(R.id.songListPageFragment, args)
    }

    private fun openYear(yearValue: String) {
        try {
            val year = yearValue.trim { it <= ' ' }.toInt()
            val args = Bundle()
            args.putInt("year_object", year)
            args.putString(Constants.MEDIA_BY_YEAR, Constants.MEDIA_BY_YEAR)
            navigateSafely(R.id.songListPageFragment, args)
        } catch (ex: NumberFormatException) {
            Toast.makeText(activity, R.string.asset_link_error_unsupported, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun navigateSafely(destinationId: Int, args: Bundle?) {
        activity.runOnUiThread(Runnable {
            val navController = activity.navController
            if (navController == null) {
                return@Runnable
            }
            if (navController.currentDestination != null
                && navController.currentDestination!!.id == destinationId
            ) {
                navController.navigate(
                    destinationId,
                    args,
                    NavOptions.Builder().setLaunchSingleTop(true).build()
                )
            } else {
                navController.navigate(destinationId, args)
            }
        })
    }
}