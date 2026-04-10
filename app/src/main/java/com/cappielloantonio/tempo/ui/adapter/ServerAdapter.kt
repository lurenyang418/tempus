package com.cappielloantonio.tempo.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.databinding.ItemLoginServerBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.model.Server

class ServerAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<ServerAdapter.ViewHolder?>() {
    private var servers: MutableList<Server>

    init {
        this.servers = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            ItemLoginServerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val server = servers[position]

        holder.item.serverNameTextView.setText(server.serverName)
        holder.item.serverAddressTextView.setText(server.address)
    }

    override fun getItemCount(): Int {
        return servers.size
    }

    fun setItems(servers: MutableList<Server>) {
        this.servers = servers
        notifyDataSetChanged()
    }

    fun getItem(id: Int): Server? {
        return servers[id]
    }

    inner class ViewHolder internal constructor(var item: ItemLoginServerBinding) :
        RecyclerView.ViewHolder(
            item.getRoot()
        ) {
        init {
            item.serverNameTextView.setSelected(true)

            itemView.setOnClickListener(View.OnClickListener { v: View? -> onClick() })
            itemView.setOnLongClickListener(OnLongClickListener { v: View? -> onLongClick() })
        }

        fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable("server_object", servers[getBindingAdapterPosition()])

            click.onServerClick(bundle)
        }

        fun onLongClick(): Boolean {
            val bundle = Bundle()
            bundle.putParcelable("server_object", servers[getBindingAdapterPosition()])

            click.onServerLongClick(bundle)

            return true
        }
    }
}
