package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemHomeGridTrackBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.model.Chronology
import com.cappielloantonio.tempo.util.Constants

class GridTrackAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<GridTrackAdapter.ViewHolder?>() {
    private var items: MutableList<Chronology?>

    init {
        this.items = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHomeGridTrackBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        from(holder.itemView.getContext(), item?.coverArtId, ResourceType.Song)
            .build()
            .into(holder.item.trackCoverImageView)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun getItem(position: Int): Chronology? {
        return items[position]
    }

    fun setItems(items: MutableList<Chronology?>) {
        this.items = items
        notifyDataSetChanged()
    }

    inner class ViewHolder internal constructor(var item: ItemHomeGridTrackBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
        }

        fun onClick() {
            val bundle = Bundle()
            bundle.putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList<Chronology?>(items))
            bundle.putBoolean(Constants.MEDIA_CHRONOLOGY, true)
            bundle.putInt(Constants.ITEM_POSITION, getBindingAdapterPosition())

            click.onMediaClick(bundle)
        }
    }
}
