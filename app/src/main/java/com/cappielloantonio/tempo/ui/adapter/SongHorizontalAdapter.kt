package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.ItemHorizontalTrackBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.DiscTitle
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader.getUri
import com.cappielloantonio.tempo.util.MappingUtil.observeExternalAudioRefresh
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.showItemRating
import com.google.common.util.concurrent.ListenableFuture
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.concurrent.ExecutionException

@UnstableApi
class SongHorizontalAdapter(
    lifecycleOwner: LifecycleOwner?,
    private val click: ClickCallback,
    private val showCoverArt: Boolean,
    private val showAlbum: Boolean,
    private val album: AlbumID3?
) : RecyclerView.Adapter<SongHorizontalAdapter.ViewHolder?>(), Filterable {
    private var songsFull: MutableList<Child?>
    private var songs: MutableList<Child?>
    private var currentFilter: String? = ""

    private var currentPlayingId: String? = null
    private var isPlaying = false
    private var currentPlayingPositions = mutableListOf<Int?>()
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    private val filtering: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<Child?> = ArrayList<Child?>()

            if (constraint == null || constraint.length == 0) {
                filteredList.addAll(songsFull)
            } else {
                val filterPattern =
                    constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                currentFilter = filterPattern

                for (item in songsFull) {
                    if (item?.title?.lowercase(Locale.getDefault())?.contains(filterPattern) == true) {
                        filteredList.add(item)
                    }
                }
            }

            val results = FilterResults()
            results.values = filteredList

            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            @Suppress("UNCHECKED_CAST")
            songs = results.values as MutableList<Child?>
            notifyDataSetChanged()

            for (pos in currentPlayingPositions) {
                if (pos!! >= 0 && pos < songs.size) {
                    notifyItemChanged(pos, "payload_playback")
                }
            }
        }
    }

    init {
        this.songs = mutableListOf()
        this.songsFull = mutableListOf()
        setHasStableIds(false)

        if (lifecycleOwner != null) {
            observeExternalAudioRefresh(
                lifecycleOwner,
                Runnable { this.handleExternalAudioRefresh() })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalTrackBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any?>) {
        if (!payloads.isEmpty() && payloads.contains("payload_playback")) {
            bindPlaybackState(holder, songs[position])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position] ?: return

        holder.item.searchResultSongTitleTextView.setText(song.title)

        holder.item.searchResultSongSubtitleTextView.setText(
            holder.itemView.getContext().getString(
                R.string.song_subtitle_formatter,
                if (this.showAlbum) song.album else song.artist,
                MusicUtil.getReadableDurationString(song.duration, false),
                MusicUtil.getReadableAudioQualityString(song)
            )
        )

        holder.item.trackNumberTextView.setText(
            MusicUtil.getReadableTrackNumber(
                holder.itemView.getContext(),
                song.track
            )
        )

        if (getDownloadDirectoryUri() == null) {
            if (DownloadUtil.getDownloadTracker(holder.itemView.getContext())
                    .isDownloaded(song.id)
            ) {
                holder.item.searchResultDownloadIndicatorImageView.setVisibility(View.VISIBLE)
            } else {
                holder.item.searchResultDownloadIndicatorImageView.setVisibility(View.GONE)
            }
        } else {
            if (getUri(song) != null) {
                holder.item.searchResultDownloadIndicatorImageView.setVisibility(View.VISIBLE)
            } else {
                holder.item.searchResultDownloadIndicatorImageView.setVisibility(View.GONE)
            }
        }

        if (showCoverArt) from(holder.itemView.getContext(), song.coverArtId, ResourceType.Song)
            .build()
            .into(holder.item.songCoverImageView)

        holder.item.trackNumberTextView.setVisibility(if (showCoverArt) View.INVISIBLE else View.VISIBLE)
        holder.item.songCoverImageView.setVisibility(if (showCoverArt) View.VISIBLE else View.INVISIBLE)

        val prevSong = if (position > 0) songs[position - 1] else null
        val currSong = songs[position]
        if (!showCoverArt &&
            (position == 0 ||
                    (position > 0 && prevSong != null && prevSong.discNumber != null && currSong?.discNumber != null && prevSong.discNumber!! < currSong.discNumber!!
                            )
                    )
        ) {
            val discNumber = currSong?.discNumber
            if (discNumber != null && discNumber.toString().isNotBlank()) {
                holder.item.discTitleTextView.setText(
                    holder.itemView.getContext().getString(
                        R.string.disc_titleless,
                        discNumber.toString()
                    )
                )
                holder.item.differentDiskDividerSector.setVisibility(View.VISIBLE)
            } else {
                holder.item.differentDiskDividerSector.setVisibility(View.GONE)
            }

            if (album?.discTitles != null) {
                val discTitle = album.discTitles!!
                    .firstOrNull { title -> title.disc == currSong?.discNumber }

                if (discTitle != null) {
                    val title = discTitle.title
                    val disc = discTitle.disc
                    if (disc != null && title != null && title.isNotEmpty()) {
                        holder.item.discTitleTextView.setText(
                            holder.itemView.getContext().getString(
                                R.string.disc_titlefull,
                                disc.toString(),
                                title
                            )
                        )
                    }
                }
            }
        }

        if (showItemRating()) {
            if (song.starred == null && song.userRating == null) {
                holder.item.ratingIndicatorImageView.setVisibility(View.GONE)
            }

            holder.item.preferredIcon.setVisibility(if (song.starred != null) View.VISIBLE else View.GONE)
            holder.item.ratingBarLayout.setVisibility(if (song.userRating != null) View.VISIBLE else View.GONE)

            if (song.userRating != null) {
                holder.item.oneStarIcon.setImageDrawable(
                    AppCompatResources.getDrawable(
                        holder.itemView.getContext(),
                        if (song.userRating!! >= 1) R.drawable.ic_star else R.drawable.ic_star_outlined
                    )
                )
                holder.item.twoStarIcon.setImageDrawable(
                    AppCompatResources.getDrawable(
                        holder.itemView.getContext(),
                        if (song.userRating!! >= 2) R.drawable.ic_star else R.drawable.ic_star_outlined
                    )
                )
                holder.item.threeStarIcon.setImageDrawable(
                    AppCompatResources.getDrawable(
                        holder.itemView.getContext(),
                        if (song.userRating!! >= 3) R.drawable.ic_star else R.drawable.ic_star_outlined
                    )
                )
                holder.item.fourStarIcon.setImageDrawable(
                    AppCompatResources.getDrawable(
                        holder.itemView.getContext(),
                        if (song.userRating!! >= 4) R.drawable.ic_star else R.drawable.ic_star_outlined
                    )
                )
                holder.item.fiveStarIcon.setImageDrawable(
                    AppCompatResources.getDrawable(
                        holder.itemView.getContext(),
                        if (song.userRating!! >= 5) R.drawable.ic_star else R.drawable.ic_star_outlined
                    )
                )
            }
        } else {
            holder.item.ratingIndicatorImageView.setVisibility(View.GONE)
        }

        bindPlaybackState(holder, song)
    }

    private fun handleExternalAudioRefresh() {
        if (getDownloadDirectoryUri() != null) {
            notifyDataSetChanged()
        }
    }

    private fun bindPlaybackState(holder: ViewHolder, song: Child?) {
        if (song == null) return
        val isCurrent = currentPlayingId != null && currentPlayingId == song.id

        if (isCurrent) {
            holder.item.playPauseIcon.setVisibility(View.VISIBLE)
            if (isPlaying) {
                holder.item.playPauseIcon.setImageResource(R.drawable.ic_pause)
            } else {
                holder.item.playPauseIcon.setImageResource(R.drawable.ic_play)
            }
            if (!showCoverArt) {
                holder.item.trackNumberTextView.setVisibility(View.INVISIBLE)
            } else {
                holder.item.coverArtOverlay.setVisibility(View.VISIBLE)
            }
        } else {
            holder.item.playPauseIcon.setVisibility(View.INVISIBLE)
            if (!showCoverArt) {
                holder.item.trackNumberTextView.setVisibility(View.VISIBLE)
            } else {
                holder.item.coverArtOverlay.setVisibility(View.INVISIBLE)
            }
        }
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    fun setItems(songs: MutableList<Child?>?) {
        this.songsFull = if (songs != null) songs else mutableListOf()
        filtering.filter(currentFilter)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setPlaybackState(mediaId: String?, playing: Boolean) {
        val oldId = this.currentPlayingId
        val oldPlaying = this.isPlaying
        val oldPositions = currentPlayingPositions

        this.currentPlayingId = mediaId
        this.isPlaying = playing

        if (oldId == mediaId && oldPlaying == playing) {
            val newPositionsCheck =
                if (mediaId != null) findPositionsById(mediaId) else mutableListOf<Int?>()
            if (oldPositions == newPositionsCheck) {
                return
            }
        }

        currentPlayingPositions =
            if (mediaId != null) findPositionsById(mediaId) else mutableListOf<Int?>()

        for (pos in oldPositions) {
            if (pos!! >= 0 && pos < songs.size) {
                notifyItemChanged(pos, "payload_playback")
            }
        }
        for (pos in currentPlayingPositions) {
            if (!oldPositions.contains(pos) && pos!! >= 0 && pos < songs.size) {
                notifyItemChanged(pos, "payload_playback")
            }
        }
    }

    private fun findPositionsById(id: String?): MutableList<Int?> {
        if (id == null) return mutableListOf<Int?>()
        val positions: MutableList<Int?> = ArrayList<Int?>()
        for (i in songs.indices) {
            if (id == songs[i]?.id) {
                positions.add(i)
            }
        }
        return positions
    }

    override fun getFilter(): Filter {
        return filtering
    }

    fun getItem(id: Int): Child? {
        return songs[id]
    }

    inner class ViewHolder internal constructor(var item: ItemHorizontalTrackBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.searchResultSongTitleTextView.setSelected(true)
            item.searchResultSongSubtitleTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })

            item.searchResultSongMoreButton.setOnClickListener(View.OnClickListener { v: View? -> onLongClick() })
        }

        fun onClick() {
            val pos = getBindingAdapterPosition()
            val tappedSong = songs[pos] ?: return

            val bundle = Bundle()
            bundle.putParcelableArrayList(
                Constants.TRACKS_OBJECT,
                ArrayList<Child?>(MusicUtil.limitPlayableMedia(songs, getBindingAdapterPosition()))
            )
            bundle.putInt(
                Constants.ITEM_POSITION,
                MusicUtil.getPlayableMediaPosition(songs, getBindingAdapterPosition())
            )

            if (tappedSong.id == currentPlayingId) {
                Log.i(
                    "SongHorizontalAdapter",
                    "Tapping on currently playing song, toggling playback"
                )
                try {
                    val mediaBrowser = mediaBrowserListenableFuture?.get()
                    Log.i(
                        "SongHorizontalAdapter",
                        "MediaBrowser retrieved, isPlaying: " + isPlaying
                    )
                    if (isPlaying) {
                        mediaBrowser?.pause()
                    } else {
                        mediaBrowser?.play()
                    }
                } catch (e: ExecutionException) {
                    Log.e("SongHorizontalAdapter", "Error getting MediaBrowser", e)
                } catch (e: InterruptedException) {
                    Log.e("SongHorizontalAdapter", "Error getting MediaBrowser", e)
                }
            } else {
                click.onMediaClick(bundle)
            }
        }

        private fun onLongClick(): Boolean {
            val song = songs[getBindingAdapterPosition()] ?: return false
            val bundle = Bundle()
            bundle.putParcelable(Constants.TRACK_OBJECT, song)
            bundle.putInt(Constants.ITEM_POSITION, getBindingAdapterPosition())

            click.onMediaLongClick(bundle)

            return true
        }
    }

    fun sort(order: String) {
        when (order) {
            Constants.MEDIA_BY_TITLE -> songs.sortWith(compareBy { it?.title })
            Constants.MEDIA_MOST_RECENTLY_STARRED -> songs.sortWith(
                compareByDescending<Child?> { it?.starred }
            )

            Constants.MEDIA_LEAST_RECENTLY_STARRED -> songs.sortWith(
                compareBy<Child?> { it?.starred }
            )
        }

        notifyDataSetChanged()
    }

    fun setMediaBrowserListenableFuture(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?) {
        this.mediaBrowserListenableFuture = mediaBrowserListenableFuture
    }
}