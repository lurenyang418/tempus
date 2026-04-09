package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLibraryCatalogueAlbumBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.util.Constants
import java.util.Collections
import java.util.Date
import java.util.Locale

class AlbumCatalogueAdapter(private val click: ClickCallback, private val showArtist: Boolean) :
    RecyclerView.Adapter<AlbumCatalogueAdapter.ViewHolder?>(), Filterable {
    private var currentFilter: String? = ""

    private val filtering: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<AlbumID3?> = ArrayList()

            if (constraint == null || constraint.length == 0) {
                filteredList.addAll(albumsFull)
            } else {
                val filterPattern =
                    constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                currentFilter = filterPattern

                for (item in albumsFull) {
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
            albums = results.values as MutableList<AlbumID3?>?
            notifyDataSetChanged()
        }
    }

    private var albums: MutableList<AlbumID3?>?
    private var albumsFull: MutableList<AlbumID3?> = mutableListOf()

    init {
        this.albums = mutableListOf()
        this.albumsFull = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryCatalogueAlbumBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albums!![position]

        holder.item.albumNameLabel.setText(album?.name)
        holder.item.artistNameLabel.setText(album?.artist)
        holder.item.artistNameLabel.setVisibility(if (showArtist) View.VISIBLE else View.GONE)

        from(holder.itemView.getContext(), album?.coverArtId, ResourceType.Album)
            .build()
            .into(holder.item.albumCatalogueCoverImageView)
    }

    override fun getItemCount(): Int {
        return albums?.size ?: 0
    }

    fun getItem(position: Int): AlbumID3? {
        return albums?.get(position)
    }

    fun setItems(albums: MutableList<AlbumID3?>) {
        this.albumsFull = ArrayList(albums)
        filtering.filter(currentFilter)
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

    inner class ViewHolder internal constructor(var item: ItemLibraryCatalogueAlbumBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.albumNameLabel.setSelected(true)
            item.artistNameLabel.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })
        }

        private fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(Constants.ALBUM_OBJECT, albums?.get(getBindingAdapterPosition()))

            click.onAlbumClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val bundle = Bundle()
            bundle.putParcelable(Constants.ALBUM_OBJECT, albums?.get(getBindingAdapterPosition()))

            click.onAlbumLongClick(bundle)

            return true
        }
    }

    fun setItemsWithoutFilter(albums: MutableList<AlbumID3?>) {
        this.albumsFull = ArrayList(albums)
        this.albums = ArrayList(albums)
        notifyDataSetChanged()
    }

    fun sort(order: String) {
        if (albums == null) return

        when (order) {
            Constants.ALBUM_ORDER_BY_NAME -> albums!!.sortWith(
                Comparator { a, b ->
                    val nameA = a?.name?.lowercase(Locale.getDefault()) ?: ""
                    val nameB = b?.name?.lowercase(Locale.getDefault()) ?: ""
                    nameA.compareTo(nameB)
                }
            )

            Constants.ALBUM_ORDER_BY_ARTIST -> albums!!.sortWith(
                Comparator { a, b ->
                    val artistA = a?.artist?.lowercase(Locale.getDefault()) ?: ""
                    val artistB = b?.artist?.lowercase(Locale.getDefault()) ?: ""
                    artistA.compareTo(artistB)
                }
            )

            Constants.ALBUM_ORDER_BY_YEAR -> albums!!.sortWith(
                Comparator { a, b -> (a?.year ?: 0).compareTo(b?.year ?: 0) }
            )

            Constants.ALBUM_ORDER_BY_RANDOM -> Collections.shuffle(albums)
            Constants.ALBUM_ORDER_BY_RECENTLY_ADDED -> {
                albums!!.sortWith(
                    Comparator { a, b -> (a?.created ?: Date(0)).compareTo(b?.created ?: Date(0)) }
                )
                Collections.reverse(albums)
            }

            Constants.ALBUM_ORDER_BY_RECENTLY_PLAYED -> {
                albums!!.sortWith(
                    Comparator { a, b -> (a?.played ?: Date(0)).compareTo(b?.played ?: Date(0)) }
                )
                Collections.reverse(albums)
            }

            Constants.ALBUM_ORDER_BY_MOST_PLAYED -> {
                albums!!.sortWith(
                    Comparator { a, b -> (a?.playCount ?: 0L).compareTo(b?.playCount ?: 0L) }
                )
                Collections.reverse(albums)
            }
        }

        notifyDataSetChanged()
    }
}