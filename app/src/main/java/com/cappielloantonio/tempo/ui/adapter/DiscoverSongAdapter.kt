package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemHomeDiscoverSongBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.TileSizeManager.Companion.instance

class DiscoverSongAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<DiscoverSongAdapter.ViewHolder?>() {
    private var songs: MutableList<Child?>

    private var widthPx = 800
    private var heightPx = 400

    init {
        this.songs = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHomeDiscoverSongBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )

        instance!!.calculateDiscoverSize(parent.getContext())
        widthPx = instance!!.getDiscoverWidthPx(parent.getContext())

        heightPx = instance!!.getDiscoverHeightPx(parent.getContext())



        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lp = holder.item.discoverSongCoverImageView.getLayoutParams()
        lp.width = widthPx
        lp.height = heightPx
        holder.item.discoverSongCoverImageView.setLayoutParams(lp)

        val song = songs[position]

        holder.item.titleDiscoverSongLabel.setText(song?.title)
        holder.item.albumDiscoverSongLabel.setText(song?.album)

        from(holder.itemView.getContext(), song?.coverArtId, ResourceType.Song)
            .build()
            .into(holder.item.discoverSongCoverImageView)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        startAnimation(holder)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    fun setItems(songs: MutableList<Child?>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    inner class ViewHolder internal constructor(var item: ItemHomeDiscoverSongBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })

            itemView.setOnLongClickListener(OnLongClickListener { v: View? ->
                onLongClick()
                true
            })
        }

        fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(Constants.TRACK_OBJECT, songs[getBindingAdapterPosition()])
            bundle.putBoolean(Constants.MEDIA_MIX, true)

            click.onMediaClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val bundle = Bundle()
            bundle.putParcelable(Constants.TRACK_OBJECT, songs[getBindingAdapterPosition()])
            click.onMediaLongClick(bundle)
            return true
        }
    }

    private fun startAnimation(holder: ViewHolder) {
        holder.item.discoverSongCoverImageView.animate()
            .setDuration(20000)
            .setStartDelay(10)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .scaleX(1.4f)
            .scaleY(1.4f)
            .start()
    }
}