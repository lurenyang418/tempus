
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
import com.cappielloantonio.tempo.databinding.FragmentPlaylistCatalogueBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.PlaylistHorizontalAdapter
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorDialog
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PlaylistCatalogueViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener

@UnstableApi
class PlaylistCatalogueFragment : Fragment(), ClickCallback {
    private var bind: FragmentPlaylistCatalogueBinding? = null
    private var activity: MainActivity? = null
    private var playlistCatalogueViewModel: PlaylistCatalogueViewModel? = null

    private var playlistHorizontalAdapter: PlaylistHorizontalAdapter? = null

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

        bind = FragmentPlaylistCatalogueBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        playlistCatalogueViewModel =
            ViewModelProvider(requireActivity()).get<PlaylistCatalogueViewModel>(
                PlaylistCatalogueViewModel::class.java
            )

        init()
        initAppBar()
        initPlaylistCatalogueView()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun init() {
        if (requireArguments().getString(Constants.PLAYLIST_ALL) != null) {
            playlistCatalogueViewModel!!.type = Constants.PLAYLIST_ALL
        } else if (requireArguments().getString(Constants.PLAYLIST_DOWNLOADED) != null) {
            playlistCatalogueViewModel!!.type = Constants.PLAYLIST_DOWNLOADED
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
                bind!!.toolbar.setTitle(R.string.playlist_catalogue_title)
            } else {
                bind!!.toolbar.setTitle(R.string.empty_string)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initPlaylistCatalogueView() {
        bind!!.playlistCatalogueRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.playlistCatalogueRecyclerView.setHasFixedSize(true)

        playlistHorizontalAdapter = PlaylistHorizontalAdapter(this)
        bind!!.playlistCatalogueRecyclerView.setAdapter(playlistHorizontalAdapter)

        if (getActivity() != null) {
            playlistCatalogueViewModel!!.getPlaylistList(getViewLifecycleOwner())
                .observe(getViewLifecycleOwner(), Observer { playlists: MutableList<Playlist?>? ->
                    if (playlists != null) playlistHorizontalAdapter!!.setItems(playlists)
                })
        }

        bind!!.playlistCatalogueRecyclerView.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            hideKeyboard(v!!)
            false
        })

        bind!!.playlistListSortImageView.setOnClickListener(View.OnClickListener { view: View? ->
            showPopupMenu(
                view,
                R.menu.sort_playlist_popup_menu
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
                playlistHorizontalAdapter!!.getFilter().filter(newText)
                return false
            }
        })

        searchView.setPadding(-32, 0, 0, 0)
    }

    private fun hideKeyboard(view: View) {
        val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
    }

    private fun showPopupMenu(view: View?, menuResource: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.getMenuInflater().inflate(menuResource, popup.getMenu())

        popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { menuItem: MenuItem? ->
            if (menuItem!!.getItemId() == R.id.menu_playlist_sort_name) {
                playlistHorizontalAdapter!!.sort(Constants.PLAYLIST_ORDER_BY_NAME)
                return@OnMenuItemClickListener true
            } else if (menuItem.getItemId() == R.id.menu_playlist_sort_random) {
                playlistHorizontalAdapter!!.sort(Constants.PLAYLIST_ORDER_BY_RANDOM)
                return@OnMenuItemClickListener true
            }
            false
        })

        popup.show()
    }

    override fun onPlaylistClick(bundle: Bundle?) {
        bundle?.putBoolean("is_offline", false)
        findNavController(requireView()).navigate(R.id.playlistPageFragment, bundle)
        hideKeyboard(requireView())
    }

    override fun onPlaylistLongClick(bundle: Bundle?) {
        val dialog = PlaylistEditorDialog(null)
        dialog.setArguments(bundle)
        dialog.show(activity!!.getSupportFragmentManager(), null)
        hideKeyboard(requireView())
    }
}