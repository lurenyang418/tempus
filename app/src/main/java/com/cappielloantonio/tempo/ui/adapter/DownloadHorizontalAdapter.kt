package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.ItemHorizontalDownloadBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Util.distinctByKey
import java.util.Objects
import java.util.stream.Collectors

@UnstableApi
class DownloadHorizontalAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<DownloadHorizontalAdapter.ViewHolder?>() {
    private var view: String
    private var filterKey: String? = null
    private var filterValue: String? = null

    private var songs: MutableList<Child?>
    var shuffling: MutableList<Child?>? = null
        private set
    private var grouped: MutableList<Child?>

    init {
        this.view = Constants.DOWNLOAD_TYPE_TRACK
        this.songs = mutableListOf()
        this.grouped = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalDownloadBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (view) {
            Constants.DOWNLOAD_TYPE_TRACK -> initTrackLayout(holder, position)
            Constants.DOWNLOAD_TYPE_ALBUM -> initAlbumLayout(holder, position)
            Constants.DOWNLOAD_TYPE_ARTIST -> initArtistLayout(holder, position)
            Constants.DOWNLOAD_TYPE_GENRE -> initGenreLayout(holder, position)
            Constants.DOWNLOAD_TYPE_YEAR -> initYearLayout(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return grouped.size
    }

    fun setItems(view: String, filterKey: String, filterValue: String?, songs: MutableList<Child?>) {
        this.view = if (filterValue != null) view else filterKey
        this.filterKey = filterKey
        this.filterValue = filterValue

        this.songs = songs
        this.grouped = groupSong(songs)
        this.shuffling = shufflingSong(ArrayList(songs))

        notifyDataSetChanged()
    }

    fun getItem(id: Int): Child? {
        return grouped[id]
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun groupSong(songs: MutableList<Child?>): MutableList<Child?> {
        when (view) {
            Constants.DOWNLOAD_TYPE_TRACK -> return filterSong(
                filterKey!!,
                filterValue,
                songs.filter { it?.id != null }.distinctBy { it?.id }.toMutableList()
            )

            Constants.DOWNLOAD_TYPE_ALBUM -> return filterSong(
                filterKey!!,
                filterValue,
                songs.filter { it?.albumId != null }.distinctBy { it?.albumId }.toMutableList()
            )

            Constants.DOWNLOAD_TYPE_ARTIST -> return filterSong(
                filterKey!!,
                filterValue,
                songs.filter { it?.artistId != null }.distinctBy { it?.artistId }.toMutableList()
            )

            Constants.DOWNLOAD_TYPE_GENRE -> return filterSong(
                filterKey!!,
                filterValue,
                songs.filter { it?.genre != null }.distinctBy { it?.genre }.toMutableList()
            )

            Constants.DOWNLOAD_TYPE_YEAR -> return filterSong(
                filterKey!!,
                filterValue,
                songs.filter { it?.year != null }.distinctBy { it?.year }.toMutableList()
            )
        }

        return mutableListOf()
    }

    private fun filterSong(
        filterKey: String,
        filterValue: String?,
        songs: MutableList<Child?>
    ): MutableList<Child?> {
        if (filterValue != null) {
            when (filterKey) {
                Constants.DOWNLOAD_TYPE_TRACK -> return songs.filter { it?.id == filterValue }.toMutableList()
                Constants.DOWNLOAD_TYPE_ALBUM -> return songs.filter { it?.albumId == filterValue }.toMutableList()
                Constants.DOWNLOAD_TYPE_GENRE -> return songs.filter { it?.genre == filterValue }.toMutableList()
                Constants.DOWNLOAD_TYPE_YEAR -> return songs.filter { it?.year == filterValue.toInt() }.toMutableList()
                Constants.DOWNLOAD_TYPE_ARTIST -> return songs.filter { it?.artistId == filterValue }.toMutableList()
            }
        }
        return songs
    }

    private fun shufflingSong(songs: MutableList<Child?>): MutableList<Child?>? {
        if (filterValue == null) {
            return songs
        }

        when (filterKey) {
            Constants.DOWNLOAD_TYPE_TRACK -> return songs.filter { it?.id == filterValue }.toMutableList()
            Constants.DOWNLOAD_TYPE_ALBUM -> return songs.filter { it?.albumId == filterValue }.toMutableList()
            Constants.DOWNLOAD_TYPE_GENRE -> return songs.filter { it?.genre == filterValue }.toMutableList()
            Constants.DOWNLOAD_TYPE_YEAR -> return songs.filter { it?.year == filterValue!!.toInt() }.toMutableList()
            Constants.DOWNLOAD_TYPE_ARTIST -> return songs.filter { it?.artistId == filterValue }.toMutableList()
            else -> return songs
        }
    }

    private fun countSong(
        filterKey: String,
        filterValue: String?,
        songs: MutableList<Child?>
    ): String {
        if (filterValue != null) {
            when (filterKey) {
                Constants.DOWNLOAD_TYPE_TRACK -> return songs.count { it?.id == filterValue }.toString()
                Constants.DOWNLOAD_TYPE_ALBUM -> return songs.count { it?.albumId == filterValue }.toString()
                Constants.DOWNLOAD_TYPE_GENRE -> return songs.count { it?.genre == filterValue }.toString()
                Constants.DOWNLOAD_TYPE_YEAR -> return songs.count { it?.year == filterValue.toInt() }.toString()
                Constants.DOWNLOAD_TYPE_ARTIST -> return songs.count { it?.artistId == filterValue }.toString()
            }
        }
        return "0"
    }

    private fun initTrackLayout(holder: ViewHolder, position: Int) {
        val song = grouped[position]!!

        holder.item.downloadedItemTitleTextView.setText(song.title)
        holder.item.downloadedItemSubtitleTextView.setText(
            holder.itemView.getContext().getString(
                R.string.song_subtitle_formatter,
                song.artist,
                MusicUtil.getReadableDurationString(song.duration, false),
                MusicUtil.getReadableAudioQualityString(song)
            )
        )

        holder.item.downloadedItemPreTextView.setText(song.album)

        from(holder.itemView.getContext(), song.coverArtId, ResourceType.Song)
            .build()
            .into(holder.item.itemCoverImageView)

        holder.item.itemCoverImageView.setVisibility(View.VISIBLE)
        holder.item.downloadedItemMoreButton.setVisibility(View.VISIBLE)
        holder.item.divider.setVisibility(View.VISIBLE)

        if (position > 0 && grouped[position - 1] != null && (grouped[position - 1]?.album != grouped[position]?.album)
        ) {
            holder.item.divider.setPadding(
                0,
                holder.itemView.getContext().getResources()
                    .getDimension(R.dimen.downloaded_item_padding).toInt(),
                0,
                0
            )
        } else {
            if (position > 0) holder.item.divider.setVisibility(View.GONE)
        }
    }

    private fun initAlbumLayout(holder: ViewHolder, position: Int) {
        val song = grouped[position]!!

        holder.item.downloadedItemTitleTextView.setText(song.album)
        holder.item.downloadedItemSubtitleTextView.setText(
            holder.itemView.getContext().getString(
                R.string.download_item_single_subtitle_formatter, countSong(
                    Constants.DOWNLOAD_TYPE_ALBUM, song.albumId, songs
                )
            )
        )
        holder.item.downloadedItemPreTextView.setText(song.artist)

        from(holder.itemView.getContext(), song.coverArtId, ResourceType.Song)
            .build()
            .into(holder.item.itemCoverImageView)

        holder.item.itemCoverImageView.setVisibility(View.VISIBLE)
        holder.item.downloadedItemMoreButton.setVisibility(View.VISIBLE)
        holder.item.divider.setVisibility(View.VISIBLE)

        if (position > 0 && grouped[position - 1] != null && (grouped[position - 1]?.artist != grouped[position]?.artist)
        ) {
            holder.item.divider.setPadding(
                0,
                holder.itemView.getContext().getResources()
                    .getDimension(R.dimen.downloaded_item_padding).toInt(),
                0,
                0
            )
        } else {
            if (position > 0) holder.item.divider.setVisibility(View.GONE)
        }
    }

    private fun initArtistLayout(holder: ViewHolder, position: Int) {
        val song = grouped[position]!!

        holder.item.downloadedItemTitleTextView.setText(song.artist)
        holder.item.downloadedItemSubtitleTextView.setText(
            holder.itemView.getContext().getString(
                R.string.download_item_single_subtitle_formatter, countSong(
                    Constants.DOWNLOAD_TYPE_ARTIST, song.artistId, songs
                )
            )
        )

        from(holder.itemView.getContext(), song.coverArtId, ResourceType.Song)
            .build()
            .into(holder.item.itemCoverImageView)

        holder.item.itemCoverImageView.setVisibility(View.VISIBLE)
        holder.item.downloadedItemMoreButton.setVisibility(View.VISIBLE)
        holder.item.divider.setVisibility(View.GONE)
    }

    private fun initGenreLayout(holder: ViewHolder, position: Int) {
        val song = grouped[position]!!

        holder.item.downloadedItemTitleTextView.setText(song.genre)
        holder.item.downloadedItemSubtitleTextView.setText(
            holder.itemView.getContext().getString(
                R.string.download_item_single_subtitle_formatter, countSong(
                    Constants.DOWNLOAD_TYPE_GENRE, song.genre, songs
                )
            )
        )

        holder.item.itemCoverImageView.setVisibility(View.GONE)
        holder.item.downloadedItemMoreButton.setVisibility(View.VISIBLE)
        holder.item.divider.setVisibility(View.GONE)
    }

    private fun initYearLayout(holder: ViewHolder, position: Int) {
        val song = grouped[position]!!

        holder.item.downloadedItemTitleTextView.setText(song.year.toString())
        holder.item.downloadedItemSubtitleTextView.setText(
            holder.itemView.getContext().getString(
                R.string.download_item_single_subtitle_formatter, countSong(
                    Constants.DOWNLOAD_TYPE_YEAR, song.year.toString(), songs
                )
            )
        )

        holder.item.itemCoverImageView.setVisibility(View.GONE)
        holder.item.downloadedItemMoreButton.setVisibility(View.VISIBLE)
        holder.item.divider.setVisibility(View.GONE)
    }

    inner class ViewHolder internal constructor(var item: ItemHorizontalDownloadBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.downloadedItemTitleTextView.setSelected(true)
            item.downloadedItemSubtitleTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })

            item.downloadedItemMoreButton.setOnClickListener(View.OnClickListener { v: View? -> onLongClick() })
        }

        fun onClick() {
            val bundle = Bundle()
            val pos = getBindingAdapterPosition()
            val currentItem = grouped[pos]!!

            when (view) {
                Constants.DOWNLOAD_TYPE_TRACK -> {
                    bundle.putParcelableArrayList(
                        Constants.TRACKS_OBJECT,
                        ArrayList<Child?>(grouped)
                    )
                    bundle.putInt(Constants.ITEM_POSITION, pos)
                    click.onMediaClick(bundle)
                }

                Constants.DOWNLOAD_TYPE_ALBUM -> {
                    bundle.putString(
                        Constants.DOWNLOAD_TYPE_ALBUM,
                        currentItem.albumId
                    )
                    click.onAlbumClick(bundle)
                }

                Constants.DOWNLOAD_TYPE_ARTIST -> {
                    bundle.putString(
                        Constants.DOWNLOAD_TYPE_ARTIST,
                        currentItem.artistId
                    )
                    click.onArtistClick(bundle)
                }

                Constants.DOWNLOAD_TYPE_GENRE -> {
                    bundle.putString(
                        Constants.DOWNLOAD_TYPE_GENRE,
                        currentItem.genre
                    )
                    click.onGenreClick(bundle)
                }

                Constants.DOWNLOAD_TYPE_YEAR -> {
                    bundle.putString(
                        Constants.DOWNLOAD_TYPE_YEAR,
                        currentItem.year.toString()
                    )
                    click.onYearClick(bundle)
                }
            }
        }

        private fun onLongClick(): Boolean {
            val filteredSongs = ArrayList<Child?>()
            val pos = getBindingAdapterPosition()
            val currentItem = grouped[pos]!!

            val bundle = Bundle()

            when (view) {
                Constants.DOWNLOAD_TYPE_TRACK -> filteredSongs.add(
                    grouped[pos]
                )

                Constants.DOWNLOAD_TYPE_ALBUM -> filteredSongs.addAll(
                    filterSong(
                        Constants.DOWNLOAD_TYPE_ALBUM,
                        currentItem.albumId,
                        songs
                    )
                )

                Constants.DOWNLOAD_TYPE_ARTIST -> filteredSongs.addAll(
                    filterSong(
                        Constants.DOWNLOAD_TYPE_ARTIST,
                        currentItem.artistId,
                        songs
                    )
                )

                Constants.DOWNLOAD_TYPE_GENRE -> filteredSongs.addAll(
                    filterSong(
                        Constants.DOWNLOAD_TYPE_GENRE,
                        currentItem.genre,
                        songs
                    )
                )

                Constants.DOWNLOAD_TYPE_YEAR -> filteredSongs.addAll(
                    filterSong(
                        Constants.DOWNLOAD_TYPE_YEAR,
                        currentItem.year.toString(),
                        songs
                    )
                )
            }

            if (filteredSongs.isEmpty()) return false

            bundle.putParcelableArrayList(
                Constants.DOWNLOAD_GROUP,
                ArrayList<Child?>(filteredSongs)
            )
            bundle.putString(
                Constants.DOWNLOAD_GROUP_TITLE,
                item.downloadedItemTitleTextView.getText().toString()
            )
            bundle.putString(
                Constants.DOWNLOAD_GROUP_SUBTITLE,
                item.downloadedItemSubtitleTextView.getText().toString()
            )
            click.onDownloadGroupLongClick(bundle)

            return true
        }
    }
}