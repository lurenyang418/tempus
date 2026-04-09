package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemHomeInternetRadioStationBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.glide.CustomGlideRequest.ResourceType
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import com.cappielloantonio.tempo.util.Constants

@UnstableApi
class InternetRadioStationAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<InternetRadioStationAdapter.ViewHolder?>() {
    private var internetRadioStations: MutableList<InternetRadioStation?>

    init {
        this.internetRadioStations = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHomeInternetRadioStationBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val internetRadioStation = internetRadioStations[position]

        holder.item.internetRadioStationTitleTextView.setText(internetRadioStation?.name)
        holder.item.internetRadioStationSubtitleTextView.setText(internetRadioStation?.streamUrl)

        var imageId = internetRadioStation?.homePageUrl
        if (imageId == null || imageId.isEmpty()) {
            imageId = internetRadioStation?.streamUrl
        }

        from(holder.itemView.getContext(), imageId, ResourceType.Radio)
            .build()
            .into(holder.item.internetRadioStationCoverImageView)
    }

    override fun getItemCount(): Int {
        return internetRadioStations.size
    }

    fun setItems(internetRadioStations: MutableList<InternetRadioStation?>) {
        this.internetRadioStations = internetRadioStations
        notifyDataSetChanged()
    }

    fun getItem(position: Int): InternetRadioStation? {
        return internetRadioStations[position]
    }

    inner class ViewHolder internal constructor(var item: ItemHomeInternetRadioStationBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.internetRadioStationTitleTextView.setSelected(true)
            item.internetRadioStationSubtitleTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })

            item.internetRadioStationMoreButton.setOnClickListener(View.OnClickListener { v: View? -> onLongClick() })
        }

        fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(
                Constants.INTERNET_RADIO_STATION_OBJECT,
                internetRadioStations[getBindingAdapterPosition()]
            )

            click.onInternetRadioStationClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val bundle = Bundle()
            bundle.putParcelable(
                Constants.INTERNET_RADIO_STATION_OBJECT,
                internetRadioStations[getBindingAdapterPosition()]
            )

            click.onInternetRadioStationLongClick(bundle)

            return true
        }
    }
}
