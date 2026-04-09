package com.cappielloantonio.tempo.ui.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
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
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentSongListPageBinding
import com.cappielloantonio.tempo.helper.recyclerview.PaginationScrollListener
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.SongHorizontalAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.viewmodel.SongListPageViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.common.util.concurrent.ListenableFuture
import java.util.Collections
import java.util.concurrent.Future
import kotlin.math.min

@UnstableApi
class SongListPageFragment : Fragment(), ClickCallback {
    private var bind: FragmentSongListPageBinding? = null
    private var activity: MainActivity? = null
    private var songListPageViewModel: SongListPageViewModel? = null
    private var playbackViewModel: PlaybackViewModel? = null

    private var songHorizontalAdapter: SongHorizontalAdapter? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    private var isLoading = true

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

        bind = FragmentSongListPageBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        songListPageViewModel =
            ViewModelProvider(requireActivity()).get<SongListPageViewModel>(SongListPageViewModel::class.java)
        playbackViewModel =
            ViewModelProvider(requireActivity()).get<PlaybackViewModel>(PlaybackViewModel::class.java)

        init()
        initAppBar()
        initButtons()
        initSongListView()

        return view
    }

    override fun onStart() {
        super.onStart()
        initializeMediaBrowser()

        @Suppress("UNCHECKED_CAST")
        MediaManager.registerPlaybackObserver(mediaBrowserListenableFuture as ListenableFuture<MediaBrowser?>?, playbackViewModel!!)
        observePlayback()
    }

    override fun onResume() {
        super.onResume()
        setMediaBrowserListenableFuture()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun init() {
        if (requireArguments().getString(Constants.MEDIA_RECENTLY_PLAYED) != null) {
            songListPageViewModel!!.title = Constants.MEDIA_RECENTLY_PLAYED
            songListPageViewModel!!.toolbarTitle =
                getString(R.string.song_list_page_recently_played)
            bind!!.pageTitleLabel.setText(R.string.song_list_page_recently_played)
        } else if (requireArguments().getString(Constants.MEDIA_MOST_PLAYED) != null) {
            songListPageViewModel!!.title = Constants.MEDIA_MOST_PLAYED
            songListPageViewModel!!.toolbarTitle = getString(R.string.song_list_page_most_played)
            bind!!.pageTitleLabel.setText(R.string.song_list_page_most_played)
        } else if (requireArguments().getString(Constants.MEDIA_RECENTLY_ADDED) != null) {
            songListPageViewModel!!.title = Constants.MEDIA_RECENTLY_ADDED
            songListPageViewModel!!.toolbarTitle = getString(R.string.song_list_page_recently_added)
            bind!!.pageTitleLabel.setText(R.string.song_list_page_recently_added)
        } else if (requireArguments().getString(Constants.MEDIA_BY_GENRE) != null) {
            songListPageViewModel!!.title = Constants.MEDIA_BY_GENRE
            songListPageViewModel!!.genre =
                requireArguments().getParcelable<Genre?>(Constants.GENRE_OBJECT)
            songListPageViewModel!!.toolbarTitle = songListPageViewModel!!.genre!!.genre
            bind!!.pageTitleLabel.setText(songListPageViewModel!!.genre!!.genre)
        } else if (requireArguments().getString(Constants.MEDIA_BY_ARTIST) != null) {
            songListPageViewModel!!.title = Constants.MEDIA_BY_ARTIST
            songListPageViewModel!!.artist =
                requireArguments().getParcelable<ArtistID3?>(Constants.ARTIST_OBJECT)
            songListPageViewModel!!.toolbarTitle =
                getString(R.string.song_list_page_top, songListPageViewModel!!.artist!!.name)
            bind!!.pageTitleLabel.setText(
                getString(
                    R.string.song_list_page_top,
                    songListPageViewModel!!.artist!!.name
                )
            )
        } else if (requireArguments().getString(Constants.MEDIA_BY_GENRES) != null) {
            songListPageViewModel!!.title = Constants.MEDIA_BY_GENRES
            songListPageViewModel!!.filters =
                requireArguments().getStringArrayList("filters_list")!!
            songListPageViewModel!!.filterNames =
                requireArguments().getStringArrayList("filter_name_list")!!
            songListPageViewModel!!.toolbarTitle = songListPageViewModel!!.filtersTitle
            bind!!.pageTitleLabel.setText(songListPageViewModel!!.filtersTitle)
        } else if (requireArguments().getString(Constants.MEDIA_BY_YEAR) != null) {
            songListPageViewModel!!.title = Constants.MEDIA_BY_YEAR
            songListPageViewModel!!.year = requireArguments().getInt("year_object")
            songListPageViewModel!!.toolbarTitle =
                getString(R.string.song_list_page_year, songListPageViewModel!!.year)
            bind!!.pageTitleLabel.setText(
                getString(
                    R.string.song_list_page_year,
                    songListPageViewModel!!.year
                )
            )
        } else if (requireArguments().getString(Constants.MEDIA_STARRED) != null) {
            songListPageViewModel!!.title = Constants.MEDIA_STARRED
            songListPageViewModel!!.toolbarTitle = getString(R.string.song_list_page_starred)
            bind!!.pageTitleLabel.setText(R.string.song_list_page_starred)
        } else if (requireArguments().getString(Constants.MEDIA_DOWNLOADED) != null) {
            songListPageViewModel!!.title = Constants.MEDIA_DOWNLOADED
            songListPageViewModel!!.toolbarTitle = getString(R.string.song_list_page_downloaded)
            bind!!.pageTitleLabel.setText(getString(R.string.song_list_page_downloaded))
        } else if (requireArguments().getParcelable<Parcelable?>(Constants.ALBUM_OBJECT) != null) {
            songListPageViewModel!!.album =
                requireArguments().getParcelable<AlbumID3?>(Constants.ALBUM_OBJECT)
            songListPageViewModel!!.title = Constants.MEDIA_FROM_ALBUM
            songListPageViewModel!!.toolbarTitle = songListPageViewModel!!.album!!.name
            bind!!.pageTitleLabel.setText(songListPageViewModel!!.album!!.name)
        }
    }

    private fun initAppBar() {
        activity!!.setSupportActionBar(bind!!.toolbar)

        if (activity!!.getSupportActionBar() != null) {
            activity!!.getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
            activity!!.getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        }

        if (bind != null) bind!!.toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? ->
            hideKeyboard(v!!)
            activity!!.navController!!.navigateUp()
        })

        if (bind != null) bind!!.appBarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout: AppBarLayout?, verticalOffset: Int ->
            if ((bind!!.albumInfoSector.getHeight() + verticalOffset) < (2 * ViewCompat.getMinimumHeight(
                    bind!!.toolbar
                ))
            ) {
                bind!!.toolbar.setTitle(songListPageViewModel!!.toolbarTitle)
            } else {
                bind!!.toolbar.setTitle(R.string.empty_string)
            }
        })
    }

    private fun initButtons() {
        songListPageViewModel!!.getSongList()
            .observe(getViewLifecycleOwner(), Observer { songs: MutableList<Child?>? ->
                if (bind != null) {
                    setSongListPageSorter()

                    bind!!.songListShuffleImageView.setOnClickListener(View.OnClickListener { v: View? ->
                        Collections.shuffle(songs)
                        MediaManager.startQueue(
                            mediaBrowserListenableFuture,
                            songs!!.subList(0, min(500, songs.size)),
                            0
                        )
                        activity!!.setBottomSheetInPeek(true)
                    })
                }
            })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSongListView() {
        bind!!.songListRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.songListRecyclerView.setHasFixedSize(true)

        songHorizontalAdapter =
            SongHorizontalAdapter(getViewLifecycleOwner(), this, true, false, null)
        bind!!.songListRecyclerView.setAdapter(songHorizontalAdapter)
        setMediaBrowserListenableFuture()
        reapplyPlayback()
        songListPageViewModel!!.getSongList()
            .observe(getViewLifecycleOwner(), Observer { songs: MutableList<Child?>? ->
                isLoading = false
                songHorizontalAdapter!!.setItems(songs)
                reapplyPlayback()
                setSongListPageSubtitle(songs!!)
            })

        bind!!.songListRecyclerView.addOnScrollListener(object :
            PaginationScrollListener((bind!!.songListRecyclerView.getLayoutManager() as LinearLayoutManager?)!!) {
            override fun loadMoreItems() {
                this@SongListPageFragment.isLoading = true
                songListPageViewModel!!.getSongsByPage(getViewLifecycleOwner())
            }

            override val isLoading: Boolean = this@SongListPageFragment.isLoading
        })

        bind!!.songListRecyclerView.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            hideKeyboard(v!!)
            false
        })

        bind!!.songListSortImageView.setOnClickListener(View.OnClickListener { view: View? ->
            showPopupMenu(
                view,
                R.menu.sort_song_popup_menu
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
                songHorizontalAdapter!!.getFilter().filter(newText)
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

        popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { menuItem: MenuItem? ->
            if (menuItem!!.getItemId() == R.id.menu_song_sort_name) {
                songHorizontalAdapter!!.sort(Constants.MEDIA_BY_TITLE)
                return@OnMenuItemClickListener true
            } else if (menuItem.getItemId() == R.id.menu_song_sort_most_recently_starred) {
                songHorizontalAdapter!!.sort(Constants.MEDIA_MOST_RECENTLY_STARRED)
                return@OnMenuItemClickListener true
            } else if (menuItem.getItemId() == R.id.menu_song_sort_least_recently_starred) {
                songHorizontalAdapter!!.sort(Constants.MEDIA_LEAST_RECENTLY_STARRED)
                return@OnMenuItemClickListener true
            }
            false
        })

        popup.show()
    }

    private fun setSongListPageSubtitle(children: MutableList<Child?>) {
        when (songListPageViewModel!!.title) {
            Constants.MEDIA_BY_GENRE -> bind!!.pageSubtitleLabel.setText(
                if (children.size < songListPageViewModel!!.maxNumberByGenre) getString(
                    R.string.generic_list_page_count,
                    children.size
                ) else getString(
                    R.string.generic_list_page_count_unknown,
                    songListPageViewModel!!.maxNumberByGenre
                )
            )

            Constants.MEDIA_BY_YEAR -> bind!!.pageSubtitleLabel.setText(
                if (children.size < songListPageViewModel!!.maxNumberByYear) getString(
                    R.string.generic_list_page_count,
                    children.size
                ) else getString(
                    R.string.generic_list_page_count_unknown,
                    songListPageViewModel!!.maxNumberByYear
                )
            )

            Constants.MEDIA_BY_ARTIST, Constants.MEDIA_BY_GENRES, Constants.MEDIA_STARRED -> bind!!.pageSubtitleLabel.setText(
                getString(R.string.generic_list_page_count, children.size)
            )
        }
    }

    private fun setSongListPageSorter() {
        when (songListPageViewModel!!.title) {
            Constants.MEDIA_BY_GENRE, Constants.MEDIA_BY_YEAR -> bind!!.songListSortImageView.setVisibility(
                View.GONE
            )

            Constants.MEDIA_BY_ARTIST, Constants.MEDIA_BY_GENRES, Constants.MEDIA_STARRED -> bind!!.songListSortImageView.setVisibility(
                View.VISIBLE
            )
        }
    }

    private fun initializeMediaBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(
                requireContext(),
                ComponentName(requireContext(), MediaService::class.java)
            )
        ).buildAsync()
    }

    private fun releaseMediaBrowser() {
        @Suppress("UNCHECKED_CAST")
        val raw: Any = mediaBrowserListenableFuture!!
        @Suppress("UNCHECKED_CAST")
        val future: Future<out androidx.media3.session.MediaController> = raw as Future<out androidx.media3.session.MediaController>
        MediaBrowser.releaseFuture(future)
    }

        override fun onMediaClick(bundle: Bundle?) {
        hideKeyboard(requireView())
        val tracks: MutableList<Child?> = bundle?.getParcelableArrayList<Child?>(
            Constants.TRACKS_OBJECT
        )?.toMutableList() ?: mutableListOf()
        MediaManager.startQueue(
            mediaBrowserListenableFuture, tracks, bundle?.getInt(Constants.ITEM_POSITION) ?: 0
        )
        activity!!.setBottomSheetInPeek(true)
    }

    override fun onMediaLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
    }

    private fun observePlayback() {
        playbackViewModel!!.getCurrentSongId()
            .observe(getViewLifecycleOwner(), Observer { id: String? ->
                if (songHorizontalAdapter != null) {
                    val playing = playbackViewModel!!.getIsPlaying().getValue()
                    songHorizontalAdapter!!.setPlaybackState(id, playing != null && playing)
                }
            })
        playbackViewModel!!.getIsPlaying()
            .observe(getViewLifecycleOwner(), Observer { playing: Boolean? ->
                if (songHorizontalAdapter != null) {
                    val id = playbackViewModel!!.getCurrentSongId().getValue()
                    songHorizontalAdapter!!.setPlaybackState(id, playing != null && playing)
                }
            })
    }

    private fun reapplyPlayback() {
        if (songHorizontalAdapter != null) {
            val id = playbackViewModel!!.getCurrentSongId().getValue()
            val playing = playbackViewModel!!.getIsPlaying().getValue()
            songHorizontalAdapter!!.setPlaybackState(id, playing != null && playing)
        }
    }

    private fun setMediaBrowserListenableFuture() {
        songHorizontalAdapter!!.setMediaBrowserListenableFuture(mediaBrowserListenableFuture)
    }

    companion object {
        private const val TAG = "SongListPageFragment"
    }
}