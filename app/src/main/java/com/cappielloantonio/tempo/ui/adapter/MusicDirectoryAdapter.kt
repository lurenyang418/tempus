package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLibraryMusicDirectoryBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants

@UnstableApi
class MusicDirectoryAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<MusicDirectoryAdapter.ViewHolder?>() {
    private var children: MutableList<Child?> = mutableListOf()

    init {
        this.children = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryMusicDirectoryBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val child = children[position]

        holder.item.musicDirectoryTitleTextView.setText(child?.title)

        val type = if (child?.isDir == true)
            ResourceType.Directory
        else
            ResourceType.Song

        from(holder.itemView.getContext(), child?.coverArtId, type)
            .build()
            .into(holder.item.musicDirectoryCoverImageView)

        holder.item.musicDirectoryMoreButton.setVisibility(if (child?.isDir == true) View.VISIBLE else View.INVISIBLE)
        holder.item.musicDirectoryPlayButton.setVisibility(if (child?.isDir == true) View.VISIBLE else View.INVISIBLE)
    }

    override fun getItemCount(): Int {
        return children.size
    }

    fun setItems(children: MutableList<Child?>?) {
        this.children = if (children != null) children else mutableListOf()
        notifyDataSetChanged()
    }

    inner class ViewHolder internal constructor(var item: ItemLibraryMusicDirectoryBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.musicDirectoryTitleTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })

            item.musicDirectoryMoreButton.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            item.musicDirectoryPlayButton.setOnClickListener(View.OnClickListener { v: View? -> onPlayClick() })
        }

        fun onClick() {
            val bundle = Bundle()

            if (children[getBindingAdapterPosition()]?.isDir == true) {
                bundle.putString(
                    Constants.MUSIC_DIRECTORY_ID,
                    children[getBindingAdapterPosition()]?.id
                )
                click.onMusicDirectoryClick(bundle)
            } else {
                bundle.putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList<Child?>(children))
                bundle.putInt(Constants.ITEM_POSITION, getBindingAdapterPosition())
                click.onMediaClick(bundle)
            }
        }

        private fun onLongClick(): Boolean {
            if (children[getBindingAdapterPosition()]?.isDir != true) {
                val bundle = Bundle()
                bundle.putParcelable(
                    Constants.TRACK_OBJECT,
                    children[getBindingAdapterPosition()]
                )

                click.onMediaLongClick(bundle)

                return true
            } else {
                return false
            }
        }

        fun onPlayClick() {
            if (children[getBindingAdapterPosition()]?.isDir == true) {
                val bundle = Bundle()
                bundle.putString(
                    Constants.MUSIC_DIRECTORY_ID,
                    children[getBindingAdapterPosition()]?.id
                )
                click.onMusicDirectoryPlay(bundle)
            }
        }
    }
}