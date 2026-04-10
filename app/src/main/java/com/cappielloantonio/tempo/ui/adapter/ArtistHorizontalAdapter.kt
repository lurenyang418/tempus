package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemHorizontalArtistBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.util.Constants
import java.util.Date
import java.util.Locale

class ArtistHorizontalAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<ArtistHorizontalAdapter.ViewHolder?>(), Filterable {
    private var artistsFull: MutableList<ArtistID3?> = mutableListOf()
    private var artists: MutableList<ArtistID3?> = mutableListOf()
    private var currentFilter: String? = ""

    private val filtering: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<ArtistID3?> = mutableListOf()

            if (constraint == null || constraint.length == 0) {
                filteredList.addAll(artistsFull)
            } else {
                val filterPattern =
                    constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                currentFilter = filterPattern

                for (item in artistsFull) {
                    if (item?.name?.lowercase(Locale.getDefault())?.contains(filterPattern) == true) {
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
            artists = results.values as MutableList<ArtistID3?>
            notifyDataSetChanged()
        }
    }

    init {
        this.artists = mutableListOf()
        this.artistsFull = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalArtistBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artists[position]

        holder.item.artistNameTextView.setText(artist?.name)

        if ((artist?.albumCount ?: 0) > 0) {
            holder.item.artistInfoTextView.setText("Album count: " + artist?.albumCount)
        } else {
            holder.item.artistInfoTextView.setVisibility(View.GONE)
        }

        from(holder.itemView.getContext(), artist?.coverArtId, ResourceType.Artist)
            .build()
            .into(holder.item.artistCoverImageView)
    }

    override fun getItemCount(): Int {
        return artists.size
    }

    fun setItems(artists: MutableList<ArtistID3?>?) {
        this.artistsFull = if (artists != null) artists else mutableListOf()
        filtering.filter(currentFilter)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return filtering
    }

    fun getItem(id: Int): ArtistID3? {
        return artists[id]
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class ViewHolder internal constructor(var item: ItemHorizontalArtistBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.artistNameTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })

            item.artistMoreButton.setOnClickListener(View.OnClickListener { v: View? -> onLongClick() })
        }

        private fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(Constants.ARTIST_OBJECT, artists[getBindingAdapterPosition()])

            click.onArtistClick(bundle)
        }

        fun onLongClick(): Boolean {
            val bundle = Bundle()
            bundle.putParcelable(Constants.ARTIST_OBJECT, artists[getBindingAdapterPosition()])

            click.onArtistLongClick(bundle)

            return true
        }
    }

    fun sort(order: String) {
        when (order) {
            Constants.ARTIST_ORDER_BY_NAME -> artists.sortWith(
                Comparator { a, b ->
                    val nameA = a?.name?.lowercase(Locale.getDefault()) ?: ""
                    val nameB = b?.name?.lowercase(Locale.getDefault()) ?: ""
                    nameA.compareTo(nameB)
                }
            )

            Constants.ARTIST_ORDER_BY_MOST_RECENTLY_STARRED -> artists.sortWith(
                Comparator { a, b ->
                    val dateA = a?.starred ?: Date(0)
                    val dateB = b?.starred ?: Date(0)
                    dateB.compareTo(dateA)
                }
            )

            Constants.ARTIST_ORDER_BY_LEAST_RECENTLY_STARRED -> artists.sortWith(
                Comparator { a, b ->
                    val dateA = a?.starred ?: Date(Long.MAX_VALUE)
                    val dateB = b?.starred ?: Date(Long.MAX_VALUE)
                    dateA.compareTo(dateB)
                }
            )

        }

        notifyDataSetChanged()
    }
}