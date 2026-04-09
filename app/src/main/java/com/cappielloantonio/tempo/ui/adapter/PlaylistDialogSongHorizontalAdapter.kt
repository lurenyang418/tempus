package com.cappielloantonio.tempo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemHorizontalPlaylistDialogTrackBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.MusicUtil

class PlaylistDialogSongHorizontalAdapter :
    RecyclerView.Adapter<PlaylistDialogSongHorizontalAdapter.ViewHolder?>() {
    private var songs: MutableList<Child?>

    init {
        this.songs = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalPlaylistDialogTrackBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]!!

        holder.item.playlistDialogSongTitleTextView.setText(song.title)
        holder.item.playlistDialogAlbumArtistTextView.setText(song.artist)
        holder.item.playlistDialogSongDurationTextView.setText(
            MusicUtil.getReadableDurationString(
                song.duration,
                false
            )
        )

        from(holder.itemView.getContext(), song.coverArtId, ResourceType.Song)
            .build()
            .into(holder.item.playlistDialogSongCoverImageView)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    var items: MutableList<Child?>
        get() = this.songs
        set(songs) {
            this.songs = songs
            notifyDataSetChanged()
        }

    fun getItem(id: Int): Child? {
        return songs[id]
    }

    class ViewHolder internal constructor(var item: ItemHorizontalPlaylistDialogTrackBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.playlistDialogSongTitleTextView.setSelected(true)
        }
    }
}
