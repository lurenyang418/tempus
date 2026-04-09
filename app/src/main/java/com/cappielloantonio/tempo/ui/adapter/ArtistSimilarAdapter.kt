package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLibrarySimilarArtistBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.SimilarArtistID3
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.TileSizeManager.Companion.instance

class ArtistSimilarAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<ArtistSimilarAdapter.ViewHolder?>() {
    private var artists: MutableList<SimilarArtistID3?>

    private var sizePx = 400

    init {
        this.artists = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibrarySimilarArtistBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )

        instance!!.calculateTileSize(parent.getContext())
        sizePx = instance!!.getTileSizePx(parent.getContext())

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lp = holder.item.similarArtistCoverImageView.getLayoutParams()
        lp.width = sizePx
        lp.height = sizePx
        holder.item.similarArtistCoverImageView.setLayoutParams(lp)

        val artist = artists[position]

        holder.item.artistNameLabel.setText(artist?.name)

        from(holder.itemView.getContext(), artist?.coverArtId, ResourceType.Artist)
            .build()
            .into(holder.item.similarArtistCoverImageView)
    }

    override fun getItemCount(): Int {
        return artists.size
    }

    fun getItem(position: Int): SimilarArtistID3? {
        return artists[position]
    }

    fun setItems(artists: MutableList<SimilarArtistID3?>) {
        this.artists = artists
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class ViewHolder internal constructor(var item: ItemLibrarySimilarArtistBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })

            item.artistNameLabel.setSelected(true)
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
}
