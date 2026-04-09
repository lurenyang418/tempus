package com.cappielloantonio.tempo.helper.recyclerview

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class PaginationScrollListener protected constructor(private val layoutManager: LinearLayoutManager) :
    RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val visibleItemCount = layoutManager.getChildCount()
        val totalItemCount = layoutManager.getItemCount()
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

        if (!this.isLoading) {
            if (firstVisibleItemPosition >= 0 && (visibleItemCount + firstVisibleItemPosition) >= (totalItemCount / 4 * 3)) {
                loadMoreItems()
            }
        }
    }

    protected abstract fun loadMoreItems()

    abstract val isLoading: Boolean
}