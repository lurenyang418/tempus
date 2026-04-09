package com.cappielloantonio.tempo.helper.recyclerview

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class CustomLinearSnapHelper : LinearSnapHelper() {
    override fun findSnapView(layoutManager: RecyclerView.LayoutManager?): View? {
        if (layoutManager is LinearLayoutManager) {
            val linearLayoutManager = layoutManager
            if (!needToDoSnap(linearLayoutManager)) {
                return null
            }
        }
        return super.findSnapView(layoutManager)
    }

    fun needToDoSnap(linearLayoutManager: LinearLayoutManager): Boolean {
        return linearLayoutManager.findFirstCompletelyVisibleItemPosition() != 0 && linearLayoutManager.findLastCompletelyVisibleItemPosition() != linearLayoutManager.getItemCount() - 1
    }
}