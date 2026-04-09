package com.cappielloantonio.tempo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemHorizontalHomeSectorBinding
import com.cappielloantonio.tempo.model.HomeSector

class HomeSectorHorizontalAdapter :
    RecyclerView.Adapter<HomeSectorHorizontalAdapter.ViewHolder?>() {
    private var sectors: MutableList<HomeSector?>

    init {
        this.sectors = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalHomeSectorBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sector = sectors[position]

        holder.item.homeSectorTitleCheckBox.setText(sector?.sectorTitle)
        holder.item.homeSectorTitleCheckBox.setChecked(sector?.isVisible == true)
    }

    override fun getItemCount(): Int {
        return sectors.size
    }

    var items: MutableList<HomeSector?>
        get() = this.sectors
        set(sectors) {
            this.sectors = sectors
            notifyDataSetChanged()
        }

    fun getItem(id: Int): HomeSector? {
        return sectors[id]
    }

    inner class ViewHolder internal constructor(var item: ItemHorizontalHomeSectorBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            this.item.homeSectorTitleCheckBox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                onCheck(
                    isChecked
                )
            })
        }

        private fun onCheck(isChecked: Boolean) {
            sectors[getBindingAdapterPosition()]?.isVisible = isChecked
        }
    }
}
