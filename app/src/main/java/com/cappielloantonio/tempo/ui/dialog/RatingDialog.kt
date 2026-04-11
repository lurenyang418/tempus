package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogRatingBinding
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.RatingViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RatingDialog : DialogFragment() {
    private var bind: DialogRatingBinding? = null
    private var ratingViewModel: RatingViewModel? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogRatingBinding.inflate(getLayoutInflater())
        ratingViewModel =
            ViewModelProvider(requireActivity()).get<RatingViewModel>(RatingViewModel::class.java)

        return MaterialAlertDialogBuilder(getActivity()!!)
            .setView(bind!!.getRoot())
            .setTitle(R.string.rating_dialog_title)
            .setNegativeButton(
                R.string.rating_dialog_negative_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })
            .setPositiveButton(
                R.string.rating_dialog_positive_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    ratingViewModel!!.rate(bind!!.ratingBar.getRating().toInt())
                })
            .create()
    }

    override fun onStart() {
        super.onStart()

        setElementInfo()
        setRating()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setElementInfo() {
        if (BundleCompat.getParcelable(
                requireArguments(),
                Constants.TRACK_OBJECT,
                Child::class.java
            ) != null
        ) {
            ratingViewModel!!.setSong(
                BundleCompat.getParcelable(requireArguments(), Constants.TRACK_OBJECT, Child::class.java)
            )
        } else if (BundleCompat.getParcelable(
                requireArguments(),
                Constants.ALBUM_OBJECT,
                AlbumID3::class.java
            ) != null
        ) {
            ratingViewModel!!.setAlbum(
                BundleCompat.getParcelable(
                    requireArguments(),
                    Constants.ALBUM_OBJECT,
                    AlbumID3::class.java
                )
            )
        } else if (BundleCompat.getParcelable(
                requireArguments(),
                Constants.ARTIST_OBJECT,
                ArtistID3::class.java
            ) != null
        ) {
            ratingViewModel!!.setArtist(
                BundleCompat.getParcelable(
                    requireArguments(),
                    Constants.ARTIST_OBJECT,
                    ArtistID3::class.java
                )
            )
        }
    }

    private fun setRating() {
        if (ratingViewModel!!.getSong() != null) {
            ratingViewModel!!.liveSong.observe(this, Observer { song: Child? ->
                bind!!.ratingBar.setRating((if (song!!.userRating != null) song.userRating else 0)!!.toFloat())
            })
        } else if (ratingViewModel!!.getAlbum() != null) {
            ratingViewModel!!.liveAlbum.observe(this, Observer { album: AlbumID3? ->
                if (album != null) {
                    bind!!.ratingBar.setRating((if (album.userRating != null) album.userRating else 0)!!.toFloat())
                }
            })
        } else if (ratingViewModel!!.getArtist() != null) {
            ratingViewModel!!.liveArtist.observe(
                this,
                Observer { artist: ArtistID3? ->
                    bind!!.ratingBar.setRating( /*artist.getRating()*/0f)
                })
        }
    }

    companion object {
        private const val TAG = "ServerSignupDialog"
    }
}
