package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLibraryMusicFolderBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import com.cappielloantonio.tempo.util.Constants

@UnstableApi
class MusicFolderAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<MusicFolderAdapter.ViewHolder?>() {
    private var musicFolders: MutableList<MusicFolder?>

    init {
        this.musicFolders = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryMusicFolderBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val musicFolder = musicFolders[position]

        holder.item.musicFolderTitleTextView.setText(musicFolder?.name)

        from(holder.itemView.getContext(), musicFolder?.name, ResourceType.Folder)
            .build()
            .into(holder.item.musicFolderCoverImageView)
    }

    override fun getItemCount(): Int {
        return musicFolders.size
    }

    fun setItems(musicFolders: MutableList<MusicFolder?>) {
        this.musicFolders = musicFolders
        notifyDataSetChanged()
    }

    fun getItem(position: Int): MusicFolder? {
        return musicFolders[position]
    }

    inner class ViewHolder internal constructor(var item: ItemLibraryMusicFolderBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.musicFolderTitleTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })

            item.musicFolderMoreButton.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
        }

        fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(
                Constants.MUSIC_FOLDER_OBJECT,
                musicFolders[getBindingAdapterPosition()]
            )
            click.onMusicFolderClick(bundle)
        }
    }
}
