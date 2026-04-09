package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLibraryArtistBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.TileSizeManager.Companion.instance

@UnstableApi
class ArtistAdapter(
    private val click: ClickCallback,
    private val mix: Boolean,
    private val bestOf: Boolean
) : RecyclerView.Adapter<ArtistAdapter.ViewHolder?>() {
    private var sizePx = 400
    private var artists: MutableList<ArtistID3?>

    init {
        this.artists = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryArtistBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )

        instance!!.calculateTileSize(parent.getContext())
        sizePx = instance!!.getTileSizePx(parent.getContext())

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lp = holder.item.artistCoverImageView.getLayoutParams()
        lp.width = sizePx
        lp.height = sizePx
        holder.item.artistCoverImageView.setLayoutParams(lp)

        val artist = artists[position]

        holder.item.artistNameLabel.setText(artist?.name)

        from(holder.itemView.getContext(), artist?.coverArtId, ResourceType.Artist)
            .build()
            .into(holder.item.artistCoverImageView)
    }

    override fun getItemCount(): Int {
        return artists.size
    }

    fun getItem(position: Int): ArtistID3? {
        return artists[position]
    }

    fun setItems(artists: MutableList<ArtistID3?>) {
        this.artists = artists
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class ViewHolder internal constructor(var item: ItemLibraryArtistBinding) :
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
            bundle.putBoolean(Constants.MEDIA_MIX, mix)
            bundle.putBoolean(Constants.MEDIA_BEST_OF, bestOf)

            click.onArtistClick(bundle)
        }

        fun onLongClick(): Boolean {
            val bundle = Bundle()
            bundle.putParcelable(Constants.ARTIST_OBJECT, artists[getBindingAdapterPosition()])

            click.onArtistLongClick(bundle)

            return true
        }
    }
}
