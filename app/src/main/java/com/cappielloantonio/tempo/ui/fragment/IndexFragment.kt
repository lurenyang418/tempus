package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.BundleCompat
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
import com.cappielloantonio.tempo.databinding.FragmentIndexBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Directory
import com.cappielloantonio.tempo.subsonic.models.Indexes
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.MusicIndexAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.IndexUtil.getArtist
import com.cappielloantonio.tempo.viewmodel.IndexViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

@UnstableApi
class IndexFragment : Fragment(), ClickCallback {
    private var bind: FragmentIndexBinding? = null
    private var activity: MainActivity? = null
    private var indexViewModel: IndexViewModel? = null

    private var musicIndexAdapter: MusicIndexAdapter? = null
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null
    private var directoryRepository: DirectoryRepository? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        bind = FragmentIndexBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        indexViewModel =
            ViewModelProvider(requireActivity()).get<IndexViewModel>(IndexViewModel::class.java)
        directoryRepository = DirectoryRepository()

        initAppBar()
        initDirectoryListView()
        init()

        return view
    }

    override fun onStart() {
        super.onStart()
        initializeMediaBrowser()
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
        val musicFolder =
            BundleCompat.getParcelable(
                requireArguments(),
                Constants.MUSIC_FOLDER_OBJECT,
                MusicFolder::class.java
            )

        if (musicFolder != null) {
            indexViewModel!!.setMusicFolder(musicFolder)
            bind!!.indexTitleLabel.setText(musicFolder.name)
        }
    }

    private fun initAppBar() {
        activity!!.setSupportActionBar(bind!!.toolbar)

        if (activity!!.getSupportActionBar() != null) {
            activity!!.getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
            activity!!.getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        }

        if (bind != null) bind!!.toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> activity!!.navController!!.navigateUp() })

        if (bind != null) bind!!.appBarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout: AppBarLayout?, verticalOffset: Int ->
            if ((bind!!.indexInfoSector.getHeight() + verticalOffset) < (2 * bind!!.toolbar.minimumHeight)
            ) {
                bind!!.toolbar.setTitle(indexViewModel!!.musicFolderName)
            } else {
                bind!!.toolbar.setTitle(R.string.empty_string)
            }
        })
    }

    private fun initDirectoryListView() {
        val musicFolder =
            BundleCompat.getParcelable(
                requireArguments(),
                Constants.MUSIC_FOLDER_OBJECT,
                MusicFolder::class.java
            )

        bind!!.indexRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.indexRecyclerView.setHasFixedSize(true)

        musicIndexAdapter = MusicIndexAdapter(this)
        bind!!.indexRecyclerView.setAdapter(musicIndexAdapter)

        indexViewModel!!.getIndexes(if (musicFolder != null) musicFolder.id else null)
            .observe(getViewLifecycleOwner(), Observer { indexes: Indexes? ->
                if (indexes != null) {
                    musicIndexAdapter!!.setItems(getArtist(indexes))
                }
            })

        bind!!.fastScrollbar.setRecyclerView(bind!!.indexRecyclerView)
        bind!!.fastScrollbar.setViewsToUse(
            R.layout.layout_fast_scrollbar,
            R.id.fastscroller_bubble,
            R.id.fastscroller_handle
        )
    }

    override fun onMusicIndexClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.directoryFragment, bundle)
    }

    override fun onMusicIndexPlay(bundle: Bundle?) {
        val directoryId = bundle?.getString(Constants.MUSIC_DIRECTORY_ID)
        if (directoryId != null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.folder_play_collecting),
                Toast.LENGTH_SHORT
            ).show()
            collectAndPlayDirectorySongs(directoryId)
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

    @Suppress("UNCHECKED_CAST")
    private fun releaseMediaBrowser() {
        val future = mediaBrowserListenableFuture as ListenableFuture<MediaController>
        MediaBrowser.releaseFuture(future)
    }

    private fun collectAndPlayDirectorySongs(directoryId: String?) {
        val allSongs: MutableList<Child?> = ArrayList<Child?>()
        val pendingRequests = AtomicInteger(0)

        collectSongsFromDirectory(directoryId, allSongs, pendingRequests, Runnable {
            if (!allSongs.isEmpty()) {
                activity!!.runOnUiThread(Runnable {
                    MediaManager.startQueue(mediaBrowserListenableFuture, allSongs, 0)
                    activity!!.setBottomSheetInPeek(true)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.folder_play_playing, allSongs.size),
                        Toast.LENGTH_SHORT
                    ).show()
                })
            } else {
                activity!!.runOnUiThread(Runnable {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.folder_play_no_songs),
                        Toast.LENGTH_SHORT
                    ).show()
                })
            }
        })
    }

    private fun collectSongsFromDirectory(
        directoryId: String?,
        allSongs: MutableList<Child?>,
        pendingRequests: AtomicInteger,
        onComplete: Runnable
    ) {
        pendingRequests.incrementAndGet()

        directoryRepository!!.getMusicDirectory(directoryId)
            .observe(getViewLifecycleOwner(), Observer { directory: Directory? ->
                if (directory != null && directory.children != null) {
                    for (child in directory.children) {
                        if (child.isDir) {
                            // It's a subdirectory, recurse into it
                            collectSongsFromDirectory(
                                child.id,
                                allSongs,
                                pendingRequests,
                                onComplete
                            )
                        } else if (!child.isVideo) {
                            // It's a song, add it to the list
                            synchronized(allSongs) {
                                allSongs.add(child)
                            }
                        }
                    }
                }
                // Decrement pending requests and check if we're done
                if (pendingRequests.decrementAndGet() == 0) {
                    onComplete.run()
                }
            })
    }

    companion object {
        private const val TAG = "IndexFragment"
    }
}