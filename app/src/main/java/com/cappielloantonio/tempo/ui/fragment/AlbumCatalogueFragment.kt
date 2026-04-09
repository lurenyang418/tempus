package com.cappielloantonio.tempo.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.SearchView
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentAlbumCatalogueBinding
import com.cappielloantonio.tempo.helper.recyclerview.GridItemDecoration
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.AlbumCatalogueAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences.getAlbumSortOrder
import com.cappielloantonio.tempo.util.Preferences.setAlbumSortOrder
import com.cappielloantonio.tempo.util.TileSizeManager.Companion.instance
import com.cappielloantonio.tempo.viewmodel.AlbumCatalogueViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener

@OptIn(markerClass = [UnstableApi::class])
class AlbumCatalogueFragment : Fragment(), ClickCallback {
    private var bind: FragmentAlbumCatalogueBinding? = null
    private var activity: MainActivity? = null
    private var albumCatalogueViewModel: AlbumCatalogueViewModel? = null
    private var spanCount = 2
    private var tileSpacing = 20
    private var albumAdapter: AlbumCatalogueAdapter? = null
    private var currentSortOrder: String? = null
    private var originalAlbums: MutableList<AlbumID3?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        currentSortOrder = getAlbumSortOrder()

        initData()
    }

    override fun onResume() {
        super.onResume()
        val latestSort = getAlbumSortOrder()

        if (latestSort != currentSortOrder) {
            currentSortOrder = latestSort
        }
        // Re-apply sort when returning to fragment
        if (originalAlbums != null && currentSortOrder != null) {
            applySortToAlbums(currentSortOrder)
        } else {
            Log.d(TAG, "onResume - Cannot re-sort, missing data")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        albumCatalogueViewModel!!.stopLoading()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        bind = FragmentAlbumCatalogueBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()

        instance!!.calculateTileSize(requireContext())
        spanCount = instance!!.getTileSpanCount(requireContext())
        tileSpacing = instance!!.getTileSpacing(requireContext())

        initAppBar()
        initAlbumCatalogueView()
        initProgressLoader()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun initData() {
        albumCatalogueViewModel = ViewModelProvider(requireActivity()).get<AlbumCatalogueViewModel>(
            AlbumCatalogueViewModel::class.java
        )
        albumCatalogueViewModel!!.loadAlbums()
    }

    private fun initAppBar() {
        activity!!.setSupportActionBar(bind!!.toolbar)

        if (activity!!.getSupportActionBar() != null) {
            activity!!.getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
            activity!!.getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        }

        bind!!.toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? ->
            hideKeyboard(v!!)
            activity!!.navController!!.navigateUp()
        })


        bind!!.appBarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout: AppBarLayout?, verticalOffset: Int ->
            if ((bind!!.albumInfoSector.getHeight() + verticalOffset) < (2 * ViewCompat.getMinimumHeight(
                    bind!!.toolbar
                ))
            ) {
                bind!!.toolbar.setTitle(R.string.album_catalogue_title)
            } else {
                bind!!.toolbar.setTitle(R.string.empty_string)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initAlbumCatalogueView() {
        bind!!.albumCatalogueRecyclerView.setLayoutManager(
            GridLayoutManager(
                requireContext(),
                spanCount
            )
        )
        bind!!.albumCatalogueRecyclerView.addItemDecoration(
            GridItemDecoration(
                spanCount,
                tileSpacing,
                false
            )
        )
        bind!!.albumCatalogueRecyclerView.setHasFixedSize(true)

        albumAdapter = AlbumCatalogueAdapter(this, true)
        albumAdapter!!.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY)
        bind!!.albumCatalogueRecyclerView.setAdapter(albumAdapter)
        albumCatalogueViewModel!!.getAlbumList()
            .observe(getViewLifecycleOwner(), Observer { albums: MutableList<AlbumID3?>? ->
                originalAlbums = albums
                currentSortOrder = getAlbumSortOrder()
                applySortToAlbums(currentSortOrder)
                updateSortIndicator()
            })

        bind!!.albumCatalogueRecyclerView.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            hideKeyboard(v!!)
            false
        })

        bind!!.albumListSortImageView.setOnClickListener(View.OnClickListener { view: View? ->
            showPopupMenu(
                view,
                R.menu.sort_album_popup_menu
            )
        })
    }

    private fun applySortToAlbums(sortOrder: String?) {
        if (originalAlbums == null) {
            return
        }
        albumAdapter!!.setItemsWithoutFilter(originalAlbums!!)
        if (sortOrder != null) {
            albumAdapter!!.sort(sortOrder)
        }
    }

    private fun initProgressLoader() {
        albumCatalogueViewModel!!.loadingStatus.observe(
            getViewLifecycleOwner(),
            Observer { isLoading: Boolean? ->
                if (isLoading == true) {
                    bind!!.albumListSortImageView.setEnabled(false)
                    bind!!.albumListProgressLoader.setVisibility(View.VISIBLE)
                } else {
                    bind!!.albumListSortImageView.setEnabled(true)
                    bind!!.albumListProgressLoader.setVisibility(View.GONE)
                }
            })
    }

    private fun updateSortIndicator() {
        if (bind == null) return

        val sortText = getSortDisplayText(currentSortOrder)
        bind!!.albumListSortTextView.setText(sortText)
        bind!!.albumListSortTextView.setVisibility(View.VISIBLE)
    }

    private fun getSortDisplayText(sortOrder: String?): String {
        if (sortOrder == null) return ""

        when (sortOrder) {
            Constants.ALBUM_ORDER_BY_NAME -> return getString(R.string.menu_sort_name)
            Constants.ALBUM_ORDER_BY_ARTIST -> return getString(R.string.menu_group_by_artist)
            Constants.ALBUM_ORDER_BY_YEAR -> return getString(R.string.menu_sort_year)
            Constants.ALBUM_ORDER_BY_RANDOM -> return getString(R.string.menu_sort_random)
            Constants.ALBUM_ORDER_BY_RECENTLY_ADDED -> return getString(R.string.menu_sort_recently_added)
            Constants.ALBUM_ORDER_BY_RECENTLY_PLAYED -> return getString(R.string.menu_sort_recently_played)
            Constants.ALBUM_ORDER_BY_MOST_PLAYED -> return getString(R.string.menu_sort_most_played)
            else -> return ""
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)

        val searchView = searchItem.getActionView() as SearchView?
        searchView!!.setImeOptions(EditorInfo.IME_ACTION_DONE)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                albumAdapter!!.getFilter().filter(newText)
                return false
            }
        })

        searchView.setPadding(-32, 0, 0, 0)
    }

    private fun hideKeyboard(view: View) {
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
    }

    private fun showPopupMenu(view: View?, menuResource: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.getMenuInflater().inflate(menuResource, popup.getMenu())

        popup.setOnMenuItemClickListener { menuItem ->
            var newSortOrder: String? = null
            if (menuItem!!.itemId == R.id.menu_album_sort_name) {
                newSortOrder = Constants.ALBUM_ORDER_BY_NAME
            } else if (menuItem.itemId == R.id.menu_album_sort_artist) {
                newSortOrder = Constants.ALBUM_ORDER_BY_ARTIST
            } else if (menuItem.itemId == R.id.menu_album_sort_year) {
                newSortOrder = Constants.ALBUM_ORDER_BY_YEAR
            } else if (menuItem.itemId == R.id.menu_album_sort_random) {
                newSortOrder = Constants.ALBUM_ORDER_BY_RANDOM
            } else if (menuItem.itemId == R.id.menu_album_sort_recently_added) {
                newSortOrder = Constants.ALBUM_ORDER_BY_RECENTLY_ADDED
            } else if (menuItem.itemId == R.id.menu_album_sort_recently_played) {
                newSortOrder = Constants.ALBUM_ORDER_BY_RECENTLY_PLAYED
            } else if (menuItem.itemId == R.id.menu_album_sort_most_played) {
                newSortOrder = Constants.ALBUM_ORDER_BY_MOST_PLAYED
            }

            if (newSortOrder != null) {
                currentSortOrder = newSortOrder
                setAlbumSortOrder(newSortOrder)
                applySortToAlbums(newSortOrder)
                updateSortIndicator()
                return@setOnMenuItemClickListener true
            }
            false
        }

        popup.show()
    }

    override fun onAlbumClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.albumPageFragment, bundle)
        hideKeyboard(requireView())
    }

    override fun onAlbumLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle)
    }

    companion object {
        private const val TAG = "AlbumCatalogueFragment"
    }
}