package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemHorizontalPodcastChannelBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil

class PodcastChannelHorizontalAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<PodcastChannelHorizontalAdapter.ViewHolder?>() {
    private var podcastChannels: MutableList<PodcastChannel?>

    init {
        this.podcastChannels = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalPodcastChannelBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcastChannel = podcastChannels[position]!!

        holder.item.podcastChannelTitleTextView.setText(podcastChannel.title)
        holder.item.podcastChannelDescriptionTextView.setText(
            MusicUtil.getReadableString(
                podcastChannel.description
            )
        )

        from(holder.itemView.getContext(), podcastChannel.coverArtId, ResourceType.Podcast)
            .build()
            .into(holder.item.podcastChannelCoverImageView)
    }

    override fun getItemCount(): Int {
        return podcastChannels.size
    }

    fun setItems(podcastChannels: MutableList<PodcastChannel?>) {
        this.podcastChannels = podcastChannels
        notifyDataSetChanged()
    }

    fun getItem(id: Int): PodcastChannel? {
        return podcastChannels[id]
    }

    inner class ViewHolder internal constructor(var item: ItemHorizontalPodcastChannelBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.podcastChannelTitleTextView.setSelected(true)
            item.podcastChannelDescriptionTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })

            item.podcastChannelMoreButton.setOnClickListener(View.OnClickListener { v: View? -> onLongClick() })
        }

        private fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(
                Constants.PODCAST_CHANNEL_OBJECT,
                podcastChannels[getBindingAdapterPosition()]
            )

            click.onPodcastChannelClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val bundle = Bundle()
            bundle.putParcelable(
                Constants.PODCAST_CHANNEL_OBJECT,
                podcastChannels[getBindingAdapterPosition()]
            )

            click.onPodcastChannelLongClick(bundle)

            return true
        }
    }
}
