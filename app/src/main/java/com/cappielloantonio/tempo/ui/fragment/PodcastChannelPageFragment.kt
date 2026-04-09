package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentPodcastChannelPageBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.PodcastEpisodeAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.UIUtil.getDividerItemDecoration
import com.cappielloantonio.tempo.viewmodel.PodcastChannelPageViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future

@UnstableApi
class PodcastChannelPageFragment : Fragment(), ClickCallback {
    private var bind: FragmentPodcastChannelPageBinding? = null
    private var activity: MainActivity? = null
    private var podcastChannelPageViewModel: PodcastChannelPageViewModel? = null

    private var podcastEpisodeAdapter: PodcastEpisodeAdapter? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        bind = FragmentPodcastChannelPageBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        podcastChannelPageViewModel =
            ViewModelProvider(requireActivity()).get<PodcastChannelPageViewModel>(
                PodcastChannelPageViewModel::class.java
            )

        init()
        initAppBar()
        initPodcastChannelInfo()
        initPodcastChannelEpisodesView()

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
        podcastChannelPageViewModel!!.setPodcastChannel(
            requireArguments().getParcelable<PodcastChannel>(
                Constants.PODCAST_CHANNEL_OBJECT
            )!!
        )
    }

    private fun initAppBar() {
        activity!!.setSupportActionBar(bind!!.toolbar)

        if (activity!!.getSupportActionBar() != null) {
            activity!!.getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
            activity!!.getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        }

        bind!!.toolbar.setTitle(podcastChannelPageViewModel!!.getPodcastChannel().title)
        bind!!.toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> activity!!.navController!!.navigateUp() })
        bind!!.toolbar.setTitle(podcastChannelPageViewModel!!.getPodcastChannel().title)
    }

    private fun initPodcastChannelInfo() {
        val normalizePodcastChannelDescription =
            MusicUtil.forceReadableString(podcastChannelPageViewModel!!.getPodcastChannel().description)

        if (bind != null) {
            bind!!.podcastChannelDescriptionTextView.setVisibility(if (!normalizePodcastChannelDescription.trim { it <= ' ' }
                    .isEmpty()) View.VISIBLE else View.GONE)
            bind!!.podcastChannelDescriptionTextView.setText(normalizePodcastChannelDescription)
            bind!!.podcastEpisodesFilterImageView.setOnClickListener(View.OnClickListener { view: View? ->
                showPopupMenu(
                    view,
                    R.menu.filter_podcast_episode_popup_menu
                )
            })
        }
    }

    private fun initPodcastChannelEpisodesView() {
        bind!!.podcastEpisodesRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.podcastEpisodesRecyclerView.addItemDecoration(getDividerItemDecoration(requireContext()))

        podcastEpisodeAdapter = PodcastEpisodeAdapter(this)
        bind!!.podcastEpisodesRecyclerView.setAdapter(podcastEpisodeAdapter)
        podcastChannelPageViewModel!!.podcastChannelEpisodes.observe(
            getViewLifecycleOwner(),
            Observer { channels: MutableList<PodcastChannel?>? ->
                if (channels == null) {
                    if (bind != null) {
                        bind!!.podcastEpisodesRecyclerView.setVisibility(View.GONE)
                    }
                } else {
                    if (bind != null) {
                        bind!!.podcastEpisodesRecyclerView.setVisibility(View.VISIBLE)
                    }

                    if (!channels.isEmpty() && channels.get(0) != null && channels.get(0)!!.episodes != null) {
                        val availableEpisode: MutableList<PodcastEpisode?>? =
                            channels.get(0)!!.episodes?.toMutableList()

                        if (bind != null && availableEpisode != null) {
                            bind!!.podcastEpisodesRecyclerView.setVisibility(if (availableEpisode.isEmpty()) View.GONE else View.VISIBLE)
                            podcastEpisodeAdapter!!.setItems(availableEpisode)
                        }
                    }
                }
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

    private fun releaseMediaBrowser() {
        @Suppress("UNCHECKED_CAST")
        val raw: Any = mediaBrowserListenableFuture!!
        @Suppress("UNCHECKED_CAST")
        val future: Future<out androidx.media3.session.MediaController> = raw as Future<out androidx.media3.session.MediaController>
        MediaBrowser.releaseFuture(future)
    }

    private fun showPopupMenu(view: View?, menuResource: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.getMenuInflater().inflate(menuResource, popup.getMenu())

        popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { menuItem: MenuItem? ->
            if (menuItem!!.getItemId() == R.id.menu_podcast_filter_download) {
                podcastEpisodeAdapter!!.sort(Constants.PODCAST_FILTER_BY_DOWNLOAD)
                return@OnMenuItemClickListener true
            } else if (menuItem.getItemId() == R.id.menu_podcast_filter_all) {
                podcastEpisodeAdapter!!.sort(Constants.PODCAST_FILTER_BY_ALL)
                return@OnMenuItemClickListener true
            }
            false
        })

        popup.show()
    }

    override fun onPodcastEpisodeClick(bundle: Bundle?) {
        MediaManager.startPodcast(
            mediaBrowserListenableFuture, bundle?.getParcelable<PodcastEpisode?>(
                Constants.PODCAST_OBJECT
            )
        )
        activity!!.setBottomSheetInPeek(true)
    }

    override fun onPodcastEpisodeLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.podcastEpisodeBottomSheetDialog, bundle)
    }

    override fun onPodcastEpisodeAltClick(bundle: Bundle?) {
        val episode = bundle?.getParcelable<PodcastEpisode?>(Constants.PODCAST_OBJECT)
        podcastChannelPageViewModel!!.requestPodcastEpisodeDownload(episode!!)

        Snackbar.make(
            requireView(),
            R.string.podcast_episode_download_request_snackbar,
            Snackbar.LENGTH_SHORT
        )
            .setAnchorView(activity!!.binding!!.bottomNavigation)
            .show()
    }
}