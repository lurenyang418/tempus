package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLibraryAlbumBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.TileSizeManager.Companion.instance

class AlbumAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<AlbumAdapter.ViewHolder?>() {
    private var albums: MutableList<AlbumID3?> = mutableListOf()

    private var sizePx = 400

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            ItemLibraryAlbumBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)

        instance!!.calculateTileSize(parent.getContext())
        sizePx = instance!!.getTileSizePx(parent.getContext())

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lp = holder.item.albumCoverImageView.getLayoutParams()
        lp.width = sizePx
        lp.height = sizePx
        holder.item.albumCoverImageView.setLayoutParams(lp)

        val album = albums[position] ?: return

        holder.item.albumNameLabel.setText(album.name)
        holder.item.artistNameLabel.setText(album.artist)

        from(holder.itemView.getContext(), album.coverArtId, ResourceType.Album)
            .build()
            .into(holder.item.albumCoverImageView)
    }

    override fun getItemCount(): Int {
        return albums.size
    }

    fun getItem(position: Int): AlbumID3? {
        return albums[position]
    }

    fun setItems(albums: MutableList<AlbumID3?>) {
        this.albums = albums
        notifyDataSetChanged()
    }

    inner class ViewHolder internal constructor(var item: ItemLibraryAlbumBinding) :
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
}
