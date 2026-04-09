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
import android.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentArtistListPageBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.ArtistHorizontalAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.ArtistListPageViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener

@UnstableApi
class ArtistListPageFragment : Fragment(), ClickCallback {
    private var bind: FragmentArtistListPageBinding? = null

    private var activity: MainActivity? = null
    private var artistListPageViewModel: ArtistListPageViewModel? = null

    private var artistHorizontalAdapter: ArtistHorizontalAdapter? = null

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

        bind = FragmentArtistListPageBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        artistListPageViewModel = ViewModelProvider(requireActivity()).get<ArtistListPageViewModel>(
            ArtistListPageViewModel::class.java
        )

        init()
        initAppBar()
        initArtistListView()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun init() {
        if (requireArguments().getString(Constants.ARTIST_STARRED) != null) {
            artistListPageViewModel!!.title = Constants.ARTIST_STARRED
            bind!!.pageTitleLabel.setText(R.string.artist_list_page_starred)
        } else if (requireArguments().getString(Constants.ARTIST_DOWNLOADED) != null) {
            artistListPageViewModel!!.title = Constants.ARTIST_DOWNLOADED
            bind!!.pageTitleLabel.setText(R.string.artist_list_page_downloaded)
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
            if ((bind!!.artistInfoSector.getHeight() + verticalOffset) < (2 * ViewCompat.getMinimumHeight(
                    bind!!.toolbar
                ))
            ) {
                bind!!.toolbar.setTitle(R.string.artist_list_page_title)
            } else {
                bind!!.toolbar.setTitle(R.string.empty_string)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initArtistListView() {
        bind!!.artistListRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.artistListRecyclerView.setHasFixedSize(true)

        artistHorizontalAdapter = ArtistHorizontalAdapter(this)
        bind!!.artistListRecyclerView.setAdapter(artistHorizontalAdapter)
        artistListPageViewModel!!.getArtistList(getViewLifecycleOwner())!!
            .observe(getViewLifecycleOwner(), Observer { artists: MutableList<ArtistID3?>? ->
                artistHorizontalAdapter!!.setItems(artists)
                setArtistListPageSubtitle(artists!!)
                setArtistListPageSorter()
            })

        bind!!.artistListRecyclerView.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            hideKeyboard(v!!)
            false
        })

        bind!!.artistListSortImageView.setOnClickListener(View.OnClickListener { view: View? ->
            showPopupMenu(
                view,
                R.menu.sort_horizontal_artist_popup_menu
            )
        })
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
                artistHorizontalAdapter!!.getFilter().filter(newText)
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
            if (menuItem!!.itemId == R.id.menu_horizontal_artist_sort_name) {
                artistHorizontalAdapter!!.sort(Constants.ARTIST_ORDER_BY_NAME)
                return@setOnMenuItemClickListener true
            } else if (menuItem.itemId == R.id.menu_horizontal_artist_sort_most_recently_starred) {
                artistHorizontalAdapter!!.sort(Constants.ARTIST_ORDER_BY_MOST_RECENTLY_STARRED)
                return@setOnMenuItemClickListener true
            } else if (menuItem.itemId == R.id.menu_horizontal_artist_sort_least_recently_starred) {
                artistHorizontalAdapter!!.sort(Constants.ARTIST_ORDER_BY_LEAST_RECENTLY_STARRED)
                return@setOnMenuItemClickListener true
            }
            false
        }

        popup.show()
    }

    private fun setArtistListPageSubtitle(artists: MutableList<ArtistID3?>) {
        when (artistListPageViewModel!!.title) {
            Constants.ARTIST_STARRED, Constants.ARTIST_DOWNLOADED -> bind!!.pageSubtitleLabel.setText(
                getString(R.string.generic_list_page_count, artists.size)
            )
        }
    }

    private fun setArtistListPageSorter() {
        when (artistListPageViewModel!!.title) {
            Constants.ARTIST_STARRED, Constants.ARTIST_DOWNLOADED -> bind!!.artistListSortImageView.setVisibility(
                View.VISIBLE
            )
        }
    }

    override fun onArtistClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.artistPageFragment, bundle)
    }

    override fun onArtistLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, bundle)
    }
}