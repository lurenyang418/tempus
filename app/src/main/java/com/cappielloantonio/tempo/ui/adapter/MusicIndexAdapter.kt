package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLibraryMusicIndexBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.helper.recyclerview.FastScrollbar.BubbleTextGetter
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Artist
import com.cappielloantonio.tempo.util.Constants
import java.util.Locale

@UnstableApi
class MusicIndexAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<MusicIndexAdapter.ViewHolder?>(), BubbleTextGetter {
    private var artists: MutableList<Artist?>? = mutableListOf()

    init {
        this.artists = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryMusicIndexBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artists!![position]

        holder.item.musicIndexTitleTextView.setText(artist?.name)

        from(holder.itemView.getContext(), artist?.name, ResourceType.Directory)
            .build()
            .into(holder.item.musicIndexCoverImageView)
    }

    override fun getItemCount(): Int {
        return artists?.size ?: 0
    }

    fun setItems(artists: MutableList<Artist?>?) {
        this.artists = artists
        notifyDataSetChanged()
    }

    override fun getTextToShowInBubble(pos: Int): String? {
        val artists = this.artists ?: return null
        if (artists.isEmpty()) return null
        return artists[pos]?.name?.uppercase(Locale.getDefault())?.get(0)?.toString()
    }

    inner class ViewHolder internal constructor(var item: ItemLibraryMusicIndexBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.musicIndexTitleTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            item.musicIndexMoreButton.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            item.musicIndexPlayButton.setOnClickListener(View.OnClickListener { v: View? -> onPlayClick() })
        }

        fun onClick() {
            val bundle = Bundle()
            bundle.putString(
                Constants.MUSIC_DIRECTORY_ID,
                artists!![getBindingAdapterPosition()]?.id
            )
            click.onMusicIndexClick(bundle)
        }

        fun onPlayClick() {
            val bundle = Bundle()
            bundle.putString(
                Constants.MUSIC_DIRECTORY_ID,
                artists!![getBindingAdapterPosition()]?.id
            )
            click.onMusicIndexPlay(bundle)
        }
    }
}