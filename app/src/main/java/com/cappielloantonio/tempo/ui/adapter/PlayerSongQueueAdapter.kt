package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.media3.session.MediaBrowser
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.ItemPlayerQueueSongBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.interfaces.MediaIndexCallback
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader.getUri
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.showItemRating
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.ConcurrentHashMap

class PlayerSongQueueAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<PlayerSongQueueAdapter.ViewHolder?>() {
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser?>? = null
    private var songs: MutableList<Child?>
    private val downloadStatusCache: MutableMap<String?, Boolean?> =
        ConcurrentHashMap<String?, Boolean?>()
    private var currentPlayingId: String? = null
    private var isPlaying = false
    private var currentPlayingPositions = mutableListOf<Int?>()

    init {
        this.songs = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemPlayerQueueSongBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[holder.getLayoutPosition()]!!

        holder.item.queueSongTitleTextView.setText(song.title)
        holder.item.queueSongSubtitleTextView.setText(
            holder.itemView.getContext().getString(
                R.string.song_subtitle_formatter,
                song.artist,
                MusicUtil.getReadableDurationString(song.duration, false),
                MusicUtil.getReadableAudioQualityString(song)
            )
        )

        val thumbnail = from(holder.itemView.getContext(), song.coverArtId, ResourceType.Song)
            .build()
            .sizeMultiplier(0.1f)

        from(holder.itemView.getContext(), song.coverArtId, ResourceType.Song)
            .build()
            .thumbnail(thumbnail)
            .into(holder.item.queueSongCoverImageView)
        MediaManager.getCurrentIndex(mediaBrowserListenableFuture, object : MediaIndexCallback {
            override fun onRecovery(index: Int) {
                if (holder.getLayoutPosition() < index) {
                    holder.item.queueSongTitleTextView.setAlpha(0.2f)
                    holder.item.queueSongSubtitleTextView.setAlpha(0.2f)
                    holder.item.ratingIndicatorImageView.setAlpha(0.2f)
                } else {
                    holder.item.queueSongTitleTextView.setAlpha(1.0f)
                    holder.item.queueSongSubtitleTextView.setAlpha(1.0f)
                    holder.item.ratingIndicatorImageView.setAlpha(1.0f)
                }
            }
        })

        var isDownloaded = false

        if (getDownloadDirectoryUri() == null) {
            val downloaderManager = DownloadUtil.getDownloadTracker(holder.itemView.getContext())
            isDownloaded = downloaderManager.isDownloaded(song.id)
        } else {
            isDownloaded = getUri(song) != null
        }

        if (isDownloaded) {
            holder.item.downloadIndicatorIcon.setVisibility(View.VISIBLE)
        } else {
            holder.item.downloadIndicatorIcon.setVisibility(View.GONE)
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
        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            mediaBrowserListenableFuture!!.addListener(
                Runnable {
                    try {
                        val mediaBrowser = mediaBrowserListenableFuture!!.get()
                        val pos = holder.getBindingAdapterPosition()
                        val s = songs[pos]!!
                        if (currentPlayingId != null && currentPlayingId == s.id) {
                            if (isPlaying) {
                                mediaBrowser?.pause()
                            } else {
                                mediaBrowser?.play()
                            }
                        } else {
                            mediaBrowser?.seekTo(pos, 0)
                            mediaBrowser?.play()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error obtaining MediaBrowser", e)
                    }
                }, MoreExecutors.directExecutor()
            )
        })
        bindPlaybackState(holder, song)
    }

    private fun bindPlaybackState(holder: ViewHolder, song: Child) {
        val isCurrent = currentPlayingId != null && currentPlayingId == song.id

        if (isCurrent) {
            holder.item.playPauseIcon.setVisibility(View.VISIBLE)
            if (isPlaying) {
                holder.item.playPauseIcon.setImageResource(R.drawable.ic_pause)
            } else {
                holder.item.playPauseIcon.setImageResource(R.drawable.ic_play)
            }
            holder.item.coverArtOverlay.setVisibility(View.VISIBLE)
        } else {
            holder.item.playPauseIcon.setVisibility(View.INVISIBLE)
            holder.item.coverArtOverlay.setVisibility(View.INVISIBLE)
        }
    }

    var items: MutableList<Child?>?
        get() = this.songs
        set(value) {
            this.songs = value ?: mutableListOf()
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setMediaBrowserListenableFuture(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser?>) {
        this.mediaBrowserListenableFuture = mediaBrowserListenableFuture
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

    fun getItem(id: Int): Child? {
        return songs[id]
    }

    inner class ViewHolder internal constructor(var item: ItemPlayerQueueSongBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.queueSongTitleTextView.setSelected(true)
            item.queueSongSubtitleTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
        }

        fun onClick() {
            val bundle = Bundle()
            bundle.putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList<Child?>(songs))
            bundle.putInt(Constants.ITEM_POSITION, getBindingAdapterPosition())

            click.onMediaClick(bundle)
        }
    }

    companion object {
        private const val TAG = "PlayerSongQueueAdapter"
    }
}