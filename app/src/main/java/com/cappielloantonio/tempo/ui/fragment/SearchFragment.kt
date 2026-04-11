package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentSearchBinding
import com.cappielloantonio.tempo.helper.recyclerview.CustomLinearSnapHelper
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.PlaylistWithSongs
import com.cappielloantonio.tempo.subsonic.models.SearchResult3
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.AlbumAdapter
import com.cappielloantonio.tempo.ui.adapter.ArtistAdapter
import com.cappielloantonio.tempo.ui.adapter.PlaylistHorizontalAdapter
import com.cappielloantonio.tempo.ui.adapter.SongHorizontalAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.viewmodel.SearchViewModel
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future

@UnstableApi
class SearchFragment : Fragment(), ClickCallback {
    private var bind: FragmentSearchBinding? = null
    private var activity: MainActivity? = null
    private var searchViewModel: SearchViewModel? = null
    private var playbackViewModel: PlaybackViewModel? = null

    private var artistAdapter: ArtistAdapter? = null
    private var albumAdapter: AlbumAdapter? = null
    private var songHorizontalAdapter: SongHorizontalAdapter? = null
    private var playlistHorizontalAdapter: PlaylistHorizontalAdapter? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = getActivity() as MainActivity?

        bind = FragmentSearchBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        searchViewModel =
            ViewModelProvider(requireActivity()).get<SearchViewModel>(SearchViewModel::class.java)
        playbackViewModel =
            ViewModelProvider(requireActivity()).get<PlaybackViewModel>(PlaybackViewModel::class.java)

        initSearchResultView()
        initSearchView()
        inputFocus()

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
        if (songHorizontalAdapter != null) setMediaBrowserListenableFuture()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun initSearchResultView() {
        // Artists
        bind!!.searchResultArtistRecyclerView.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        bind!!.searchResultArtistRecyclerView.setHasFixedSize(true)

        artistAdapter = ArtistAdapter(this, false, false)
        bind!!.searchResultArtistRecyclerView.setAdapter(artistAdapter)

        val artistSnapHelper = CustomLinearSnapHelper()
        artistSnapHelper.attachToRecyclerView(bind!!.searchResultArtistRecyclerView)

        // Albums
        bind!!.searchResultAlbumRecyclerView.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        bind!!.searchResultAlbumRecyclerView.setHasFixedSize(true)

        albumAdapter = AlbumAdapter(this)
        bind!!.searchResultAlbumRecyclerView.setAdapter(albumAdapter)

        val albumSnapHelper = CustomLinearSnapHelper()
        albumSnapHelper.attachToRecyclerView(bind!!.searchResultAlbumRecyclerView)

        // Songs
        bind!!.searchResultTracksRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.searchResultTracksRecyclerView.setHasFixedSize(true)

        songHorizontalAdapter =
            SongHorizontalAdapter(getViewLifecycleOwner(), this, true, false, null)
        setMediaBrowserListenableFuture()
        reapplyPlayback()

        bind!!.searchResultTracksRecyclerView.setAdapter(songHorizontalAdapter)

        bind!!.allsongsview.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.allsongsview.setHasFixedSize(true)

        playlistHorizontalAdapter = PlaylistHorizontalAdapter(this)
        bind!!.allsongsview.setAdapter(playlistHorizontalAdapter)
    }

    private fun initSearchView() {
        setRecentSuggestions()

        bind!!.searchView
            .getEditText()
            .setOnEditorActionListener(OnEditorActionListener { textView: TextView?, actionId: Int, keyEvent: KeyEvent? ->
                val query = bind!!.searchView.getText().toString()
                if (isQueryValid(query)) {
                    search(query)
                    return@OnEditorActionListener true
                }
                false
            })

        bind!!.searchView
            .getEditText()
            .addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    charSequence: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (start + count > 1) {
                        setSearchSuggestions(charSequence.toString())
                    } else {
                        setRecentSuggestions()
                    }
                }

                override fun afterTextChanged(editable: Editable?) {
                }
            })
    }

    fun setRecentSuggestions() {
        bind!!.searchViewSuggestionContainer.removeAllViews()

        for (suggestion in searchViewModel!!.recentSearchSuggestion) {
            val view = LayoutInflater.from(bind!!.searchViewSuggestionContainer.getContext())
                .inflate(
                    R.layout.item_search_suggestion,
                    bind!!.searchViewSuggestionContainer,
                    false
                )

            val leadingImageView = view.findViewById<ImageView>(R.id.search_suggestion_icon)
            val titleView = view.findViewById<TextView>(R.id.search_suggestion_title)
            val tailingImageView = view.findViewById<ImageView>(R.id.search_suggestion_delete_icon)

            leadingImageView.setImageDrawable(
                getResources().getDrawable(
                    R.drawable.ic_history,
                    null
                )
            )
            titleView.setText(suggestion)

            view.setOnClickListener(View.OnClickListener { v: View? -> search(suggestion) })

            tailingImageView.setOnClickListener(View.OnClickListener { v: View? ->
                searchViewModel!!.deleteRecentSearch(suggestion!!)
                setRecentSuggestions()
            })

            bind!!.searchViewSuggestionContainer.addView(view)
        }
    }

    fun setSearchSuggestions(query: String?) {
        searchViewModel!!.getSearchSuggestion(query)
            .observe(getViewLifecycleOwner(), Observer { suggestions: MutableList<String?>? ->
                bind!!.searchViewSuggestionContainer.removeAllViews()
                for (suggestion in suggestions!!) {
                    val view =
                        LayoutInflater.from(bind!!.searchViewSuggestionContainer.getContext())
                            .inflate(
                                R.layout.item_search_suggestion,
                                bind!!.searchViewSuggestionContainer,
                                false
                            )

                    val leadingImageView = view.findViewById<ImageView>(R.id.search_suggestion_icon)
                    val titleView = view.findViewById<TextView>(R.id.search_suggestion_title)
                    val tailingImageView =
                        view.findViewById<ImageView>(R.id.search_suggestion_delete_icon)

                    leadingImageView.setImageDrawable(
                        getResources().getDrawable(
                            R.drawable.ic_search,
                            null
                        )
                    )
                    titleView.setText(suggestion)
                    tailingImageView.setVisibility(View.GONE)

                    view.setOnClickListener(View.OnClickListener { v: View? -> search(suggestion) })

                    bind!!.searchViewSuggestionContainer.addView(view)
                }
            })
    }

    fun search(query: String?) {
        searchViewModel!!.query = query
        bind!!.allSongs.setText(
            this.getView()!!.getContext().getString(R.string.search_all_songs_loading)
        )
        playlistHorizontalAdapter!!.setItems(mutableListOf<Playlist?>())
        bind!!.searchBar.setText(query)
        bind!!.searchView.hide()
        performSearch(query)
    }

    fun updateUI(allSongs: MutableList<Playlist?>) {
        if (!allSongs.isEmpty()) {
            playlistHorizontalAdapter!!.setItems(allSongs)
        } else {
            playlistHorizontalAdapter!!.setItems(mutableListOf<Playlist?>())
        }
        bind!!.allSongs.setText(
            this.getView()!!.getContext()
                .getString(R.string.search_all_songs_play, allSongs.first()?.name ?: "")
        )
    }

    private fun performSearch(query: String?) {
        searchViewModel!!.search3(this, query)
            .observe(getViewLifecycleOwner(), Observer { result: SearchResult3? ->
                if (bind != null) {
                    if (result!!.artists != null) {
                        bind!!.searchArtistSector.setVisibility(if (!result.artists!!.isEmpty()) View.VISIBLE else View.GONE)
                        artistAdapter!!.setItems(result.artists?.toMutableList() ?: mutableListOf())
                    } else {
                        artistAdapter!!.setItems(mutableListOf<ArtistID3?>())
                        bind!!.searchArtistSector.setVisibility(View.GONE)
                    }

                    if (result.albums != null) {
                        bind!!.searchAlbumSector.setVisibility(if (!result.albums!!.isEmpty()) View.VISIBLE else View.GONE)
                        albumAdapter!!.setItems(result.albums?.toMutableList() ?: mutableListOf())
                    } else {
                        albumAdapter!!.setItems(mutableListOf<AlbumID3?>())
                        bind!!.searchAlbumSector.setVisibility(View.GONE)
                    }

                    if (result.songs != null) {
                        bind!!.searchSongSector.setVisibility(if (!result.songs!!.isEmpty()) View.VISIBLE else View.GONE)
                        songHorizontalAdapter!!.setItems(result.songs?.toMutableList() ?: mutableListOf())
                    } else {
                        songHorizontalAdapter!!.setItems(mutableListOf<Child?>())
                        bind!!.searchSongSector.setVisibility(View.GONE)
                    }
                }
            })

        bind!!.searchResultLayout.setVisibility(View.VISIBLE)
    }

    private fun isQueryValid(query: String): Boolean {
        return query != "" && query.trim { it <= ' ' }.length > 1
    }

    private fun inputFocus() {
        bind!!.searchView.show()
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

    @Suppress("UNCHECKED_CAST")
    private fun releaseMediaBrowser() {
        val future = mediaBrowserListenableFuture as ListenableFuture<MediaController>
        MediaBrowser.releaseFuture(future)
    }

    override fun onMediaClick(bundle: Bundle?) {
        val tracks: MutableList<Child?> = bundle?.getParcelableArrayList<Child?>(
            Constants.TRACKS_OBJECT
        )?.toMutableList() ?: mutableListOf()
        MediaManager.startQueue(
            mediaBrowserListenableFuture, tracks, bundle?.getInt(Constants.ITEM_POSITION) ?: 0
        )
        songHorizontalAdapter!!.notifyDataSetChanged()
        activity!!.setBottomSheetInPeek(true)
    }

    override fun onMediaLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
    }

    override fun onPlaylistClick(bundle: Bundle?) {
        val playlistWithSongs = bundle?.getParcelable<PlaylistWithSongs?>(Constants.PLAYLIST_OBJECT)
        if (playlistWithSongs != null) {
            val entries: MutableList<Child?> = playlistWithSongs.entries?.toMutableList() ?: mutableListOf()
            MediaManager.startQueue(mediaBrowserListenableFuture, entries, 0)
        }
    }

    override fun onPlaylistLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.playlistBottomSheetDialog, bundle)
    }

    override fun onAlbumClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.albumPageFragment, bundle)
    }

    override fun onAlbumLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle)
    }

    override fun onArtistClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.artistPageFragment, bundle)
    }

    override fun onArtistLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, bundle)
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
        private const val TAG = "SearchFragment"
    }
}
