package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.ItemHorizontalPlaylistDialogBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil

class PlaylistDialogHorizontalAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<PlaylistDialogHorizontalAdapter.ViewHolder?>() {
    private var playlists: MutableList<Playlist?>

    init {
        this.playlists = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalPlaylistDialogBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]!!

        holder.item.playlistDialogTitleTextView.setText(playlist.name)
        holder.item.playlistDialogCountTextView.setText(
            holder.itemView.getContext().getString(
                R.string.playlist_counted_tracks,
                playlist.songCount,
                MusicUtil.getReadableDurationString(playlist.duration, false)
            )
        )
    }

    override fun getItemCount(): Int {
        return playlists.size
    }

    fun setItems(playlists: MutableList<Playlist?>) {
        this.playlists = playlists
        notifyDataSetChanged()
    }

    fun getItem(id: Int): Playlist? {
        return playlists[id]
    }

    inner class ViewHolder internal constructor(var item: ItemHorizontalPlaylistDialogBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.playlistDialogTitleTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
        }

        fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(
                Constants.PLAYLIST_OBJECT,
                playlists[getBindingAdapterPosition()]
            )

            click.onPlaylistClick(bundle)
        }
    }
}
