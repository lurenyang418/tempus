
package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import com.cappielloantonio.tempo.databinding.FragmentDirectoryBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.interfaces.DialogClickCallback
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Directory
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.MusicDirectoryAdapter
import com.cappielloantonio.tempo.ui.dialog.DownloadDirectoryDialog
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioWriter.downloadToUserDirectory
import com.cappielloantonio.tempo.util.MappingUtil.mapDownloads
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.viewmodel.DirectoryViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.stream.Collectors

@UnstableApi
class DirectoryFragment : Fragment(), ClickCallback {
    private var bind: FragmentDirectoryBinding? = null
    private var activity: MainActivity? = null
    private var directoryViewModel: DirectoryViewModel? = null

    private var musicDirectoryAdapter: MusicDirectoryAdapter? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null
    private var directoryRepository: DirectoryRepository? = null

    private var menuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.directory_page_menu, menu)

        menuItem = menu.getItem(0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        bind = FragmentDirectoryBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        directoryViewModel =
            ViewModelProvider(requireActivity()).get<DirectoryViewModel>(DirectoryViewModel::class.java)
        directoryRepository = DirectoryRepository()

        initAppBar()
        initDirectoryListView()

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.action_download_directory) {
            val dialog = DownloadDirectoryDialog(object : DialogClickCallback {
                override fun onPositiveClick() {
                    directoryViewModel!!.loadMusicDirectory(getArguments()!!.getString(Constants.MUSIC_DIRECTORY_ID))
                        .observe(getViewLifecycleOwner(), Observer { directory: Directory? ->
                            if (isVisible() && getActivity() != null) {
                                val songs = directory!!.children!!.filter { !it.isDir }.toMutableList()
                                if (getDownloadDirectoryUri() == null) {
                                    DownloadUtil.getDownloadTracker(requireContext()).download(
                                        mapDownloads(songs),
                                        ArrayList(songs.map { Download(it) })
                                    )
                                } else {
                                    songs.forEach(Consumer { child: Child? ->
                                        downloadToUserDirectory(
                                            requireContext(),
                                            child
                                        )
                                    })
                                }
                            }
                        })
                }
            })

            dialog.show(activity!!.getSupportFragmentManager(), null)

            return true
        }

        return false
    }

    private fun initAppBar() {
        activity!!.setSupportActionBar(bind!!.toolbar)

        if (activity!!.getSupportActionBar() != null) {
            activity!!.getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
            activity!!.getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        }

        if (bind != null) {
            bind!!.toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> activity!!.navController!!.navigateUp() })
            bind!!.directoryBackImageView.setOnClickListener(View.OnClickListener { v: View? -> activity!!.navController!!.navigateUp() })
        }
    }

    private fun initDirectoryListView() {
        bind!!.directoryRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.directoryRecyclerView.setHasFixedSize(true)

        musicDirectoryAdapter = MusicDirectoryAdapter(this)
        bind!!.directoryRecyclerView.setAdapter(musicDirectoryAdapter)
        directoryViewModel!!.loadMusicDirectory(getArguments()!!.getString(Constants.MUSIC_DIRECTORY_ID))
            .observe(getViewLifecycleOwner(), Observer { directory: Directory? ->
                bind!!.appBarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout: AppBarLayout?, verticalOffset: Int ->
                    if ((bind!!.directoryInfoSector.getHeight() + verticalOffset) < (2 * bind!!.toolbar.minimumHeight)
                    ) {
                        bind!!.toolbar.setTitle(directory!!.name)
                    } else {
                        bind!!.toolbar.setTitle(R.string.empty_string)
                    }
                })
                bind!!.directoryTitleLabel.setText(directory!!.name)

                musicDirectoryAdapter!!.setItems(directory.children?.filterNotNull()?.toMutableList() ?: mutableListOf())
                menuItem!!.setVisible(
                    directory.children != null && directory.children!!
                        .any { !it.isDir }
                )
            })
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

    @Suppress("UNCHECKED_CAST")
    override fun onMediaClick(bundle: Bundle?) {
        val args = bundle ?: return
        val tracks = BundleCompat.getParcelableArrayList(args, Constants.TRACKS_OBJECT, Child::class.java)
            ?: return
        MediaManager.startQueue(
            mediaBrowserListenableFuture,
            tracks,
            args.getInt(Constants.ITEM_POSITION)
        )
    }

    override fun onMediaLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
    }

    override fun onMusicDirectoryClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.directoryFragment, bundle)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onMusicDirectoryPlay(bundle: Bundle?) {
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

    private fun collectAndPlayDirectorySongs(directoryId: String?) {
        val allSongs: MutableList<Child?> = ArrayList<Child?>()
        val pendingRequests = AtomicInteger(0)

        collectSongsFromDirectory(directoryId, allSongs, pendingRequests, Runnable {
            if (!allSongs.isEmpty()) {
                activity!!.runOnUiThread(Runnable {
                    @Suppress("UNCHECKED_CAST")
                    MediaManager.startQueue(mediaBrowserListenableFuture, allSongs.filterNotNull().toMutableList(), 0)
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
        private const val TAG = "DirectoryFragment"
    }
}