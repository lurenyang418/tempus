package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.ItemHorizontalShareBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.UIUtil.getReadableDate

class ShareHorizontalAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<ShareHorizontalAdapter.ViewHolder?>() {
    private var shares: MutableList<Share?>

    init {
        this.shares = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalShareBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val share = shares[position] ?: return

        holder.item.shareTitleTextView.setText(share.description)
        holder.item.shareSubtitleTextView.setText(
            holder.itemView.getContext()
                .getString(R.string.share_subtitle_item, getReadableDate(share.expires))
        )

        if (share.entries != null && share.entries!!.isNotEmpty()) from(
            holder.itemView.getContext(),
            share.entries!![0].coverArtId,
            ResourceType.Album
        )
            .build()
            .into(holder.item.shareCoverImageView)
    }

    override fun getItemCount(): Int {
        return shares.size
    }

    fun setItems(shares: MutableList<Share?>) {
        this.shares = shares
        notifyDataSetChanged()
    }

    fun getItem(id: Int): Share? {
        return shares[id]
    }

    inner class ViewHolder internal constructor(var item: ItemHorizontalShareBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.shareTitleTextView.setSelected(true)
            item.shareSubtitleTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })

            item.shareButton.setOnClickListener(View.OnClickListener { v: View? -> onLongClick() })
        }

        private fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(Constants.SHARE_OBJECT, shares[getBindingAdapterPosition()])

            click.onShareClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val bundle = Bundle()
            bundle.putParcelable(Constants.SHARE_OBJECT, shares[getBindingAdapterPosition()])

            click.onShareLongClick(bundle)

            return true
        }
    }
}
