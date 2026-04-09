package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLibraryGenreBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.util.Constants

class GenreAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<GenreAdapter.ViewHolder?>() {
    private var genres: MutableList<Genre?>

    init {
        this.genres = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            ItemLibraryGenreBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = genres[position]

        holder.item.genreLabel.setText(genre?.genre)
    }

    override fun getItemCount(): Int {
        return genres.size
    }

    fun getItem(position: Int): Genre? {
        return genres[position]
    }

    fun setItems(genres: MutableList<Genre?>) {
        this.genres = genres
        notifyDataSetChanged()
    }

    inner class ViewHolder internal constructor(var item: ItemLibraryGenreBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
        }

        private fun onClick() {
            val bundle = Bundle()
            bundle.putString(Constants.MEDIA_BY_GENRE, Constants.MEDIA_BY_GENRE)
            bundle.putParcelable(Constants.GENRE_OBJECT, genres[getBindingAdapterPosition()])

            click.onGenreClick(bundle)
        }
    }
}
