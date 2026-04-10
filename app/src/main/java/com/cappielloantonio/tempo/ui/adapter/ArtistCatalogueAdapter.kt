package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLibraryCatalogueArtistBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.util.Constants
import java.util.Collections
import java.util.Locale

class ArtistCatalogueAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<ArtistCatalogueAdapter.ViewHolder?>(), Filterable {
    private val filtering: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<ArtistID3?> = mutableListOf()

            if (constraint == null || constraint.length == 0) {
                filteredList.addAll(artistFull!!)
            } else {
                val filterPattern =
                    constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }

                for (item in artistFull!!) {
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
            artists.clear()
            if (results.count > 0) {
                @Suppress("UNCHECKED_CAST")
                artists.addAll(results.values as? MutableList<ArtistID3?> ?: emptyList())
            }
            notifyDataSetChanged()
        }
    }

    private var artists: MutableList<ArtistID3?> = mutableListOf()
    private var artistFull: MutableList<ArtistID3?>? = null

    init {
        this.artists = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryCatalogueArtistBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artists[position]

        holder.item.artistNameLabel.setText(artist?.name)

        from(holder.itemView.getContext(), artist?.coverArtId, ResourceType.Artist)
            .build()
            .into(holder.item.artistCatalogueCoverImageView)
    }

    override fun getItemCount(): Int {
        return artists.size
    }

    fun getItem(position: Int): ArtistID3? {
        return artists[position]
    }

    fun setItems(artists: MutableList<ArtistID3?>) {
        this.artists = artists
        this.artistFull = artists.toMutableList()
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

    inner class ViewHolder internal constructor(var item: ItemLibraryCatalogueArtistBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.artistNameLabel.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })
        }

        fun onClick() {
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

            Constants.ARTIST_ORDER_BY_RANDOM -> Collections.shuffle(artists)
            Constants.ARTIST_ORDER_BY_ALBUM_COUNT -> artists.sortWith(
                Comparator { a, b -> (b?.albumCount ?: 0).compareTo(a?.albumCount ?: 0) }
            )
        }

        notifyDataSetChanged()
    }
}