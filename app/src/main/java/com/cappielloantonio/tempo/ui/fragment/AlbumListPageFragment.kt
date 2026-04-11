
package com.cappielloantonio.tempo.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
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
import androidx.annotation.OptIn
import androidx.appcompat.widget.SearchView
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentAlbumListPageBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.AlbumHorizontalAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.AlbumListPageViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener

@OptIn(markerClass = [UnstableApi::class])
class AlbumListPageFragment : Fragment(), ClickCallback {
    private var bind: FragmentAlbumListPageBinding? = null

    private var activity: MainActivity? = null
    private var albumListPageViewModel: AlbumListPageViewModel? = null
    private var albumHorizontalAdapter: AlbumHorizontalAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        bind = FragmentAlbumListPageBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        albumListPageViewModel =
            ViewModelProvider(requireActivity()).get<AlbumListPageViewModel>(AlbumListPageViewModel::class.java)

        init()
        initAppBar()
        initAlbumListView()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun init() {
        if (requireArguments().getString(Constants.ALBUM_RECENTLY_PLAYED) != null) {
            albumListPageViewModel!!.title = Constants.ALBUM_RECENTLY_PLAYED
            bind!!.pageTitleLabel.setText(R.string.album_list_page_recently_played)
        } else if (requireArguments().getString(Constants.ALBUM_MOST_PLAYED) != null) {
            albumListPageViewModel!!.title = Constants.ALBUM_MOST_PLAYED
            bind!!.pageTitleLabel.setText(R.string.album_list_page_most_played)
        } else if (requireArguments().getString(Constants.ALBUM_RECENTLY_ADDED) != null) {
            albumListPageViewModel!!.title = Constants.ALBUM_RECENTLY_ADDED
            bind!!.pageTitleLabel.setText(R.string.album_list_page_recently_added)
        } else if (requireArguments().getString(Constants.ALBUM_STARRED) != null) {
            albumListPageViewModel!!.title = Constants.ALBUM_STARRED
            bind!!.pageTitleLabel.setText(R.string.album_list_page_starred)
        } else if (requireArguments().getString(Constants.ALBUM_NEW_RELEASES) != null) {
            albumListPageViewModel!!.title = Constants.ALBUM_NEW_RELEASES
            bind!!.pageTitleLabel.setText(R.string.album_list_page_new_releases)
        } else if (requireArguments().getString(Constants.ALBUM_DOWNLOADED) != null) {
            albumListPageViewModel!!.title = Constants.ALBUM_DOWNLOADED
            bind!!.pageTitleLabel.setText(R.string.album_list_page_downloaded)
        } else if (BundleCompat.getParcelable(
                requireArguments(),
                Constants.ARTIST_OBJECT,
                ArtistID3::class.java
            ) != null
        ) {
            albumListPageViewModel!!.artist =
                BundleCompat.getParcelable(
                    requireArguments(),
                    Constants.ARTIST_OBJECT,
                    ArtistID3::class.java
                )
            albumListPageViewModel!!.title = Constants.ALBUM_FROM_ARTIST
            bind!!.pageTitleLabel.setText(albumListPageViewModel!!.artist!!.name)
        }
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
            if ((bind!!.albumInfoSector.getHeight() + verticalOffset) < (2 * bind!!.toolbar.minimumHeight)
            ) {
                bind!!.toolbar.setTitle(R.string.album_list_page_title)
            } else {
                bind!!.toolbar.setTitle(R.string.empty_string)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initAlbumListView() {
        bind!!.albumListRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.albumListRecyclerView.setHasFixedSize(true)

        albumHorizontalAdapter = AlbumHorizontalAdapter(
            this,
            (albumListPageViewModel!!.title == Constants.ALBUM_DOWNLOADED || albumListPageViewModel!!.title == Constants.ALBUM_FROM_ARTIST)
        )

        bind!!.albumListRecyclerView.setAdapter(albumHorizontalAdapter)
        albumListPageViewModel!!.getAlbumList(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { albums: MutableList<AlbumID3?>? ->
                albumHorizontalAdapter!!.setItems(albums)
                setAlbumListPageSubtitle(albums!!)
                setAlbumListPageSorter()
            })

        bind!!.albumListRecyclerView.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            hideKeyboard(v!!)
            false
        })

        bind!!.albumListSortImageView.setOnClickListener(View.OnClickListener { view: View? ->
            showPopupMenu(
                view,
                R.menu.sort_horizontal_album_popup_menu
            )
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.artist_list_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)

        val searchView = searchItem.getActionView() as SearchView?
        searchView!!.setImeOptions(EditorInfo.IME_ACTION_DONE)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                albumHorizontalAdapter!!.getFilter().filter(newText)
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
            if (menuItem!!.itemId == R.id.menu_horizontal_album_sort_name) {
                albumHorizontalAdapter!!.sort(Constants.ALBUM_ORDER_BY_NAME)
                return@setOnMenuItemClickListener true
            } else if (menuItem.itemId == R.id.menu_horizontal_album_sort_most_recently_starred) {
                albumHorizontalAdapter!!.sort(Constants.ALBUM_ORDER_BY_MOST_RECENTLY_STARRED)
                return@setOnMenuItemClickListener true
            } else if (menuItem.itemId == R.id.menu_horizontal_album_sort_least_recently_starred) {
                albumHorizontalAdapter!!.sort(Constants.ALBUM_ORDER_BY_LEAST_RECENTLY_STARRED)
                return@setOnMenuItemClickListener true
            }
            false
        }

        popup.show()
    }

    private fun setAlbumListPageSubtitle(albums: MutableList<AlbumID3?>) {
        when (albumListPageViewModel!!.title) {
            Constants.ALBUM_RECENTLY_PLAYED, Constants.ALBUM_MOST_PLAYED, Constants.ALBUM_RECENTLY_ADDED -> bind!!.pageSubtitleLabel.setText(
                if (albums.size < albumListPageViewModel!!.maxNumber) getString(
                    R.string.generic_list_page_count,
                    albums.size
                ) else getString(
                    R.string.generic_list_page_count_unknown,
                    albumListPageViewModel!!.maxNumber
                )
            )

            Constants.ALBUM_STARRED -> bind!!.pageSubtitleLabel.setText(
                getString(
                    R.string.generic_list_page_count,
                    albums.size
                )
            )
        }
    }

    private fun setAlbumListPageSorter() {
        when (albumListPageViewModel!!.title) {
            Constants.ALBUM_RECENTLY_PLAYED, Constants.ALBUM_MOST_PLAYED, Constants.ALBUM_RECENTLY_ADDED -> bind!!.albumListSortImageView.setVisibility(
                View.GONE
            )

            Constants.ALBUM_STARRED -> bind!!.albumListSortImageView.setVisibility(View.VISIBLE)
        }
    }

    override fun onAlbumClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.albumPageFragment, bundle)
    }

    override fun onAlbumLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle)
    }
}