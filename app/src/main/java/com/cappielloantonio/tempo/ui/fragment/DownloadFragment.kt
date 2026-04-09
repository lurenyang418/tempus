package com.cappielloantonio.tempo.ui.fragment

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentDownloadBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.model.DownloadStack
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.DownloadHorizontalAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.ExternalAudioReader.refreshCache
import com.cappielloantonio.tempo.util.Preferences.setDefaultDownloadViewType
import com.cappielloantonio.tempo.util.Preferences.setDownloadDirectoryUri
import com.cappielloantonio.tempo.viewmodel.DownloadViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.common.util.concurrent.ListenableFuture
import java.util.Collections
import java.util.Objects
import java.util.concurrent.Future

@UnstableApi
class DownloadFragment : Fragment(), ClickCallback {
    private var bind: FragmentDownloadBinding? = null
    private var activity: MainActivity? = null
    private var downloadViewModel: DownloadViewModel? = null

    private var downloadHorizontalAdapter: DownloadHorizontalAdapter? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    private var materialToolbar: MaterialToolbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = getActivity() as MainActivity?

        bind = FragmentDownloadBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        downloadViewModel =
            ViewModelProvider(requireActivity()).get<DownloadViewModel>(DownloadViewModel::class.java)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAppBar()
        initDownloadedView()
    }

    override fun onStart() {
        super.onStart()

        initializeMediaBrowser()
        activity!!.toggleBottomNavigationBarVisibilityOnOrientationChange()
        activity!!.setBottomSheetVisibility(true)
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun initAppBar() {
        materialToolbar = bind!!.getRoot().findViewById<MaterialToolbar>(R.id.toolbar)

        activity!!.setSupportActionBar(materialToolbar)
        Objects.requireNonNull<Drawable?>(materialToolbar!!.getOverflowIcon())
            .setTint(requireContext().getResources().getColor(R.color.titleTextColor, null))
    }

    private fun initDownloadedView() {
        bind!!.downloadedRecyclerView.setHasFixedSize(true)

        downloadHorizontalAdapter = DownloadHorizontalAdapter(this)
        bind!!.downloadedRecyclerView.setAdapter(downloadHorizontalAdapter)

        downloadViewModel!!.getDownloadedTracks(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { songs: MutableList<Child?>? ->
                if (songs != null) {
                    if (songs.isEmpty()) {
                        if (bind != null) {
                            bind!!.emptyDownloadLayout.setVisibility(View.VISIBLE)
                            bind!!.downloadDownloadedSector.setVisibility(View.GONE)
                            bind!!.downloadedGroupByImageView.setVisibility(View.GONE)
                        }
                    } else {
                        if (bind != null) {
                            bind!!.emptyDownloadLayout.setVisibility(View.GONE)
                            bind!!.downloadDownloadedSector.setVisibility(View.VISIBLE)
                            bind!!.downloadedGroupByImageView.setVisibility(View.VISIBLE)

                            finishDownloadView(songs)
                        }
                    }

                    if (bind != null) bind!!.loadingProgressBar.setVisibility(View.GONE)
                }
            })

        downloadViewModel!!.getRefreshResult()
            .observe(getViewLifecycleOwner(), Observer { count: Int? ->
                if (count == null || bind == null) {
                    return@Observer
                }
                if (count == -1) {
                    Toast.makeText(
                        requireContext(),
                        R.string.download_refresh_no_directory,
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (count == 0) {
                    Toast.makeText(
                        requireContext(),
                        R.string.download_refresh_no_changes,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getResources().getQuantityString(
                            R.plurals.download_refresh_removed,
                            count,
                            count
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        bind!!.downloadedGroupByImageView.setOnClickListener { view ->
            showPopupMenu(
                view,
                R.menu.download_popup_menu
            )
        }
        bind!!.downloadedGoBackImageView.setOnClickListener { downloadViewModel!!.popViewStack() }
        bind!!.downloadedRefreshImageView.setOnClickListener { downloadViewModel!!.refreshExternalDownloads() }
    }

    private fun finishDownloadView(songs: MutableList<Child?>) {
        downloadViewModel!!.getViewStack()
            .observe(getViewLifecycleOwner(), Observer { stack: ArrayList<DownloadStack?>? ->
                bind!!.downloadedRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
                val lastLevel = stack!!.get(stack.size - 1)

                when (lastLevel!!.id) {
                    Constants.DOWNLOAD_TYPE_TRACK -> downloadHorizontalAdapter!!.setItems(
                        Constants.DOWNLOAD_TYPE_TRACK,
                        lastLevel.id,
                        lastLevel.view,
                        songs
                    )

                    Constants.DOWNLOAD_TYPE_ALBUM -> downloadHorizontalAdapter!!.setItems(
                        Constants.DOWNLOAD_TYPE_TRACK,
                        lastLevel.id,
                        lastLevel.view,
                        songs
                    )

                    Constants.DOWNLOAD_TYPE_ARTIST -> downloadHorizontalAdapter!!.setItems(
                        Constants.DOWNLOAD_TYPE_ALBUM,
                        lastLevel.id,
                        lastLevel.view,
                        songs
                    )

                    Constants.DOWNLOAD_TYPE_GENRE -> downloadHorizontalAdapter!!.setItems(
                        Constants.DOWNLOAD_TYPE_TRACK,
                        lastLevel.id,
                        lastLevel.view,
                        songs
                    )

                    Constants.DOWNLOAD_TYPE_YEAR -> downloadHorizontalAdapter!!.setItems(
                        Constants.DOWNLOAD_TYPE_TRACK,
                        lastLevel.id,
                        lastLevel.view,
                        songs
                    )
                }

                bind!!.downloadedGoBackImageView.setVisibility(if (stack.size > 1) View.VISIBLE else View.GONE)

                setupBackPressing(stack.size)
                setupShuffleButton()
            })
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupShuffleButton() {
        bind!!.shuffleDownloadedTextViewClickable.setOnClickListener {
            val songs = downloadHorizontalAdapter!!.shuffling
            if (songs != null && !songs.isEmpty()) {
                Collections.shuffle(songs)
                MediaManager.startQueue(mediaBrowserListenableFuture as ListenableFuture<MediaBrowser>?, songs, 0)
                activity!!.setBottomSheetInPeek(true)
            }
        }
    }

    private fun setupBackPressing(stackSize: Int) {
        requireActivity().onBackPressedDispatcher
            .addCallback(getViewLifecycleOwner(), object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (stackSize > 1) {
                        downloadViewModel!!.popViewStack()
                    } else {
                        activity!!.navController!!.navigateUp()
                    }

                    remove()
                }
            })
    }

    private fun showPopupMenu(view: View?, menuResource: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.getMenuInflater().inflate(menuResource, popup.getMenu())

        popup.setOnMenuItemClickListener { menuItem ->
            if (menuItem!!.itemId == R.id.menu_download_group_by_track) {
                downloadViewModel!!.initViewStack(
                    DownloadStack(
                        Constants.DOWNLOAD_TYPE_TRACK,
                        null
                    )
                )
                setDefaultDownloadViewType(Constants.DOWNLOAD_TYPE_TRACK)
                return@setOnMenuItemClickListener true
            } else if (menuItem.itemId == R.id.menu_download_group_by_album) {
                downloadViewModel!!.initViewStack(
                    DownloadStack(
                        Constants.DOWNLOAD_TYPE_ALBUM,
                        null
                    )
                )
                setDefaultDownloadViewType(Constants.DOWNLOAD_TYPE_ALBUM)
                return@setOnMenuItemClickListener true
            } else if (menuItem.itemId == R.id.menu_download_group_by_artist) {
                downloadViewModel!!.initViewStack(
                    DownloadStack(
                        Constants.DOWNLOAD_TYPE_ARTIST,
                        null
                    )
                )
                setDefaultDownloadViewType(Constants.DOWNLOAD_TYPE_ARTIST)
                return@setOnMenuItemClickListener true
            } else if (menuItem.itemId == R.id.menu_download_group_by_genre) {
                downloadViewModel!!.initViewStack(
                    DownloadStack(
                        Constants.DOWNLOAD_TYPE_GENRE,
                        null
                    )
                )
                setDefaultDownloadViewType(Constants.DOWNLOAD_TYPE_GENRE)
                return@setOnMenuItemClickListener true
            } else if (menuItem.itemId == R.id.menu_download_group_by_year) {
                downloadViewModel!!.initViewStack(DownloadStack(Constants.DOWNLOAD_TYPE_YEAR, null))
                setDefaultDownloadViewType(Constants.DOWNLOAD_TYPE_YEAR)
                return@setOnMenuItemClickListener true
            } else if (menuItem.itemId == R.id.menu_download_set_directory) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, REQUEST_CODE_PICK_DIRECTORY)
                return@setOnMenuItemClickListener true
            }
            false
        }

        popup.show()
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
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        val raw: Any = mediaBrowserListenableFuture!!
        @Suppress("UNCHECKED_CAST")
        val future: Future<out androidx.media3.session.MediaController> = raw as Future<out androidx.media3.session.MediaController>
        MediaBrowser.releaseFuture(future)
    }

    override fun onYearClick(bundle: Bundle?) {
        downloadViewModel!!.pushViewStack(
            DownloadStack(
                Constants.DOWNLOAD_TYPE_YEAR, bundle?.getString(
                    Constants.DOWNLOAD_TYPE_YEAR
                )
            )
        )
    }

    override fun onGenreClick(bundle: Bundle?) {
        downloadViewModel!!.pushViewStack(
            DownloadStack(
                Constants.DOWNLOAD_TYPE_GENRE, bundle?.getString(
                    Constants.DOWNLOAD_TYPE_GENRE
                )
            )
        )
    }

    override fun onArtistClick(bundle: Bundle?) {
        downloadViewModel!!.pushViewStack(
            DownloadStack(
                Constants.DOWNLOAD_TYPE_ARTIST, bundle?.getString(
                    Constants.DOWNLOAD_TYPE_ARTIST
                )
            )
        )
    }

    override fun onAlbumClick(bundle: Bundle?) {
        downloadViewModel!!.pushViewStack(
            DownloadStack(
                Constants.DOWNLOAD_TYPE_ALBUM, bundle?.getString(
                    Constants.DOWNLOAD_TYPE_ALBUM
                )
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun onMediaClick(bundle: Bundle?) {
        MediaManager.startQueue(
            mediaBrowserListenableFuture as ListenableFuture<MediaBrowser>?, bundle?.getParcelableArrayList<Child?>(
                Constants.TRACKS_OBJECT
            )!!, bundle?.getInt(Constants.ITEM_POSITION) ?: 0
        )
        activity?.setBottomSheetInPeek(true)
    }

    override fun onMediaLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
    }

    override fun onDownloadGroupLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.downloadBottomSheetDialog, bundle)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_DIRECTORY && resultCode == Activity.RESULT_OK) {
            val uri = data?.getData()
            if (uri != null) {
                requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                setDownloadDirectoryUri(uri.toString())
                refreshCache()
                Toast.makeText(requireContext(), "Download directory set", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    companion object {
        private const val TAG = "DownloadFragment"
        private const val REQUEST_CODE_PICK_DIRECTORY = 1002
    }
}
