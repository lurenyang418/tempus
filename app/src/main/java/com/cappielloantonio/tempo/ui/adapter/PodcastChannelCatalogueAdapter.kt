package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemHomeCataloguePodcastChannelBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.util.Constants
import java.util.Locale

class PodcastChannelCatalogueAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<PodcastChannelCatalogueAdapter.ViewHolder?>(), Filterable {
    private val filtering: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<PodcastChannel?> = ArrayList<PodcastChannel?>()

            if (constraint == null || constraint.length == 0) {
                filteredList.addAll(podcastChannelsFull!!)
            } else {
                val filterPattern =
                    constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }

                for (item in podcastChannelsFull!!) {
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
            podcastChannels.clear()
            if (results.count > 0 && results.values != null) {
                @Suppress("UNCHECKED_CAST")
                podcastChannels.addAll(results.values as MutableList<PodcastChannel>)
            }
            notifyDataSetChanged()
        }
    }

    private var podcastChannels: MutableList<PodcastChannel?>
    private var podcastChannelsFull: MutableList<PodcastChannel?>? = null

    init {
        this.podcastChannels = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHomeCataloguePodcastChannelBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcastChannel = podcastChannels[position]!!

        holder.item.podcastChannelTitleLabel.setText(podcastChannel.title)

        from(holder.itemView.getContext(), podcastChannel.coverArtId, ResourceType.Podcast)
            .build()
            .into(holder.item.podcastChannelCatalogueCoverImageView)
    }

    override fun getItemCount(): Int {
        return podcastChannels.size
    }

    fun getItem(position: Int): PodcastChannel? {
        return podcastChannels[position]
    }

    fun setItems(podcastChannels: MutableList<PodcastChannel?>) {
        this.podcastChannels = podcastChannels
        this.podcastChannelsFull = ArrayList(podcastChannels)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getFilter(): Filter {
        return filtering
    }

    inner class ViewHolder internal constructor(var item: ItemHomeCataloguePodcastChannelBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.podcastChannelTitleLabel.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })
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
