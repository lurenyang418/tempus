package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentHomeTabPodcastBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.interfaces.PodcastCallback
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.PodcastChannelHorizontalAdapter
import com.cappielloantonio.tempo.ui.adapter.PodcastEpisodeAdapter
import com.cappielloantonio.tempo.ui.dialog.PodcastChannelEditorDialog
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences.setPodcastSectionHidden
import com.cappielloantonio.tempo.util.UIUtil.getDividerItemDecoration
import com.cappielloantonio.tempo.viewmodel.PodcastViewModel
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future
import java.util.stream.Collectors

@UnstableApi
class HomeTabPodcastFragment : Fragment(), ClickCallback, PodcastCallback {
    private var bind: FragmentHomeTabPodcastBinding? = null
    private var activity: MainActivity? = null
    private var podcastViewModel: PodcastViewModel? = null

    private var podcastEpisodeAdapter: PodcastEpisodeAdapter? = null
    private var podcastChannelHorizontalAdapter: PodcastChannelHorizontalAdapter? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = getActivity() as MainActivity?

        bind = FragmentHomeTabPodcastBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        podcastViewModel =
            ViewModelProvider(requireActivity()).get<PodcastViewModel>(PodcastViewModel::class.java)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        initPodcastView()
        initNewestPodcastsView()
        initPodcastChannelsView()
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
        bind!!.podcastChannelsPreTextView.setOnClickListener(View.OnClickListener { v: View? ->
            val dialog = PodcastChannelEditorDialog(this)
            dialog.show(activity!!.getSupportFragmentManager(), null)
        })

        bind!!.podcastChannelsTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            activity!!.navController!!.navigate(
                R.id.action_homeFragment_to_podcastChannelCatalogueFragment
            )
        })
        bind!!.hideSectionButton.setOnClickListener(View.OnClickListener { v: View? -> setPodcastSectionHidden() })
    }

    private fun initPodcastView() {
        podcastViewModel!!.getPodcastChannels(getViewLifecycleOwner()).observe(
            getViewLifecycleOwner(),
            Observer { podcastChannels: MutableList<PodcastChannel?>? ->
                if (podcastChannels == null) {
                    if (bind != null) bind!!.homePodcastChannelsSector.setVisibility(View.GONE)
                    if (bind != null) bind!!.emptyPodcastLayout.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homePodcastChannelsSector.setVisibility(if (!podcastChannels.isEmpty()) View.VISIBLE else View.GONE)
                    if (bind != null) bind!!.emptyPodcastLayout.setVisibility(if (podcastChannels.isEmpty()) View.VISIBLE else View.GONE)
                }
            })
    }

    private fun initPodcastChannelsView() {
        bind!!.podcastChannelsRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))

        podcastChannelHorizontalAdapter = PodcastChannelHorizontalAdapter(this)
        bind!!.podcastChannelsRecyclerView.setAdapter(podcastChannelHorizontalAdapter)
        podcastViewModel!!.getPodcastChannels(getViewLifecycleOwner()).observe(
            getViewLifecycleOwner(),
            Observer { podcastChannels: MutableList<PodcastChannel?>? ->
                if (podcastChannels == null) {
                    if (bind != null) bind!!.homePodcastChannelsSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homePodcastChannelsSector.setVisibility(if (!podcastChannels.isEmpty()) View.VISIBLE else View.GONE)

                    podcastChannelHorizontalAdapter!!.setItems(podcastChannels)
                }
            })
    }

    private fun initNewestPodcastsView() {
        bind!!.newestPodcastsRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.newestPodcastsRecyclerView.addItemDecoration(getDividerItemDecoration(requireContext()))

        podcastEpisodeAdapter = PodcastEpisodeAdapter(this)
        bind!!.newestPodcastsRecyclerView.setAdapter(podcastEpisodeAdapter)
        podcastViewModel!!.getNewestPodcastEpisodes(getViewLifecycleOwner()).observe(
            getViewLifecycleOwner(),
            Observer { podcastEpisodes: MutableList<PodcastEpisode?>? ->
                if (podcastEpisodes == null) {
                    if (bind != null) bind!!.homeNewestPodcastsSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeNewestPodcastsSector.setVisibility(if (!podcastEpisodes.isEmpty()) View.VISIBLE else View.GONE)

                    podcastEpisodeAdapter!!.setItems(
                        podcastEpisodes.filter { it?.status == "completed" }.toMutableList()
                    )
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

    @Suppress("UNCHECKED_CAST")
    private fun releaseMediaBrowser() {
        val future = mediaBrowserListenableFuture as ListenableFuture<MediaController>
        MediaBrowser.releaseFuture(future)
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

    override fun onPodcastChannelClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.podcastChannelPageFragment, bundle)
    }

    override fun onPodcastChannelLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.podcastChannelBottomSheetDialog, bundle)
    }

    override fun onDismiss() {
        Handler().postDelayed(Runnable {
            if (podcastViewModel != null) podcastViewModel!!.refreshPodcastChannels(
                getViewLifecycleOwner()
            )
            if (podcastViewModel != null) podcastViewModel!!.refreshNewestPodcastEpisodes(
                getViewLifecycleOwner()
            )
        }, 1000)
    }

    companion object {
        private const val TAG = "HomeTabPodcastFragment"
    }
}
