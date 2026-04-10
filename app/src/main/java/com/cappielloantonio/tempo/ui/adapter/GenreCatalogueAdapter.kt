package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLibraryCatalogueGenreBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.util.Constants
import java.util.Collections
import java.util.Locale

class GenreCatalogueAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<GenreCatalogueAdapter.ViewHolder?>(), Filterable {
    private val filtering: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<Genre?> = mutableListOf()

            if (constraint == null || constraint.length == 0) {
                filteredList.addAll(genresFull!!)
            } else {
                val filterPattern =
                    constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }

                for (item in genresFull!!) {
                    if (item?.genre?.lowercase(Locale.getDefault())?.contains(filterPattern) == true) {
                        filteredList.add(item)
                    }
                }
            }

            val results = FilterResults()
            results.values = filteredList

            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            genres.clear()
            if (results.count > 0) {
                @Suppress("UNCHECKED_CAST")
                genres.addAll(results.values as? MutableList<Genre?> ?: emptyList())
            }
            notifyDataSetChanged()
        }
    }

    private var genres: MutableList<Genre?> = mutableListOf()
    private var genresFull: MutableList<Genre?>? = null

    init {
        this.genres = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryCatalogueGenreBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
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
        this.genresFull = ArrayList(genres).toMutableList()
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return filtering
    }

    inner class ViewHolder internal constructor(var item: ItemLibraryCatalogueGenreBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                val bundle = Bundle()
                bundle.putString(Constants.MEDIA_BY_GENRE, Constants.MEDIA_BY_GENRE)
                bundle.putParcelable(
                    Constants.GENRE_OBJECT,
                    genres[getBindingAdapterPosition()]
                )
                click.onGenreClick(bundle)
            })
        }
    }

    fun sort(order: String) {
        when (order) {
            Constants.GENRE_ORDER_BY_NAME -> genres.sortWith(
                Comparator { a, b ->
                    val genreA = a?.genre ?: ""
                    val genreB = b?.genre ?: ""
                    genreA.compareTo(genreB)
                }
            )
            Constants.GENRE_ORDER_BY_RANDOM -> Collections.shuffle(genres)
        }

        notifyDataSetChanged()
    }
}