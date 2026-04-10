package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemHorizontalAlbumBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.util.Constants
import java.util.Date
import java.util.Locale

class AlbumHorizontalAdapter(private val click: ClickCallback, private val isOffline: Boolean) :
    RecyclerView.Adapter<AlbumHorizontalAdapter.ViewHolder?>(), Filterable {
    private var albumsFull: MutableList<AlbumID3?> = mutableListOf()
    private var albums: MutableList<AlbumID3?> = mutableListOf()
    private var currentFilter: String? = ""

    private val filtering: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<AlbumID3?> = mutableListOf()

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
            albums = results.values as MutableList<AlbumID3?>
            notifyDataSetChanged()
        }
    }

    init {
        this.albums = mutableListOf()
        this.albumsFull = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalAlbumBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albums[position]

        holder.item.albumTitleTextView.setText(album?.name)
        holder.item.albumArtistTextView.setText(album?.artist)

        from(holder.itemView.getContext(), album?.coverArtId, ResourceType.Album)
            .build()
            .into(holder.item.albumCoverImageView)
    }

    override fun getItemCount(): Int {
        return albums.size
    }

    fun setItems(albums: MutableList<AlbumID3?>?) {
        this.albumsFull = if (albums != null) albums else mutableListOf()
        filtering.filter(currentFilter)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return filtering
    }

    fun getItem(id: Int): AlbumID3? {
        return albums[id]
    }

    inner class ViewHolder internal constructor(var item: ItemHorizontalAlbumBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.albumTitleTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })

            item.albumMoreButton.setOnClickListener(View.OnClickListener { v: View? -> onLongClick() })
        }

        private fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(Constants.ALBUM_OBJECT, albums[getBindingAdapterPosition()])

            click.onAlbumClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val bundle = Bundle()
            bundle.putParcelable(Constants.ALBUM_OBJECT, albums[getBindingAdapterPosition()])

            click.onAlbumLongClick(bundle)

            return true
        }
    }

    fun sort(order: String) {
        when (order) {
            Constants.ALBUM_ORDER_BY_NAME -> albums.sortWith(
                Comparator { a, b ->
                    val nameA = a?.name?.lowercase(Locale.getDefault()) ?: ""
                    val nameB = b?.name?.lowercase(Locale.getDefault()) ?: ""
                    nameA.compareTo(nameB)
                }
            )

            Constants.ALBUM_ORDER_BY_MOST_RECENTLY_STARRED -> albums.sortWith(
                Comparator { a, b ->
                    val dateA = a?.starred ?: Date(0)
                    val dateB = b?.starred ?: Date(0)
                    dateB.compareTo(dateA)
                }
            )

            Constants.ALBUM_ORDER_BY_LEAST_RECENTLY_STARRED -> albums.sortWith(
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