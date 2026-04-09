package com.cappielloantonio.tempo.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentPodcastChannelCatalogueBinding
import com.cappielloantonio.tempo.helper.recyclerview.GridItemDecoration
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.PodcastChannelCatalogueAdapter
import com.cappielloantonio.tempo.viewmodel.PodcastChannelCatalogueViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener

@OptIn(markerClass = [UnstableApi::class])
class PodcastChannelCatalogueFragment : Fragment(), ClickCallback {
    private var bind: FragmentPodcastChannelCatalogueBinding? = null
    private var activity: MainActivity? = null
    private var podcastChannelCatalogueViewModel: PodcastChannelCatalogueViewModel? = null

    private var podcastChannelCatalogueAdapter: PodcastChannelCatalogueAdapter? = null

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

        bind = FragmentPodcastChannelCatalogueBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        podcastChannelCatalogueViewModel =
            ViewModelProvider(requireActivity()).get<PodcastChannelCatalogueViewModel>(
                PodcastChannelCatalogueViewModel::class.java
            )

        initAppBar()
        initPodcastChannelCatalogueView()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
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
            if ((bind!!.podcastChannelInfoSector.getHeight() + verticalOffset) < (2 * ViewCompat.getMinimumHeight(
                    bind!!.toolbar
                ))
            ) {
                bind!!.toolbar.setTitle(R.string.podcast_channel_catalogue_title)
            } else {
                bind!!.toolbar.setTitle(R.string.empty_string)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initPodcastChannelCatalogueView() {
        bind!!.podcastChannelCatalogueRecyclerView.setLayoutManager(
            GridLayoutManager(
                requireContext(),
                2
            )
        )
        bind!!.podcastChannelCatalogueRecyclerView.addItemDecoration(
            GridItemDecoration(
                2,
                20,
                false
            )
        )
        bind!!.podcastChannelCatalogueRecyclerView.setHasFixedSize(true)

        podcastChannelCatalogueAdapter = PodcastChannelCatalogueAdapter(this)
        podcastChannelCatalogueAdapter!!.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY)
        bind!!.podcastChannelCatalogueRecyclerView.setAdapter(podcastChannelCatalogueAdapter)
        podcastChannelCatalogueViewModel!!.getPodcastChannels(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { albums: MutableList<PodcastChannel?>? ->
                if (albums != null) {
                    podcastChannelCatalogueAdapter!!.setItems(albums)
                }
            })

        bind!!.podcastChannelCatalogueRecyclerView.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            hideKeyboard(v!!)
            false
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
                podcastChannelCatalogueAdapter!!.getFilter().filter(newText)
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

    override fun onPodcastChannelClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.podcastChannelPageFragment, bundle)
        hideKeyboard(requireView())
    }

    override fun onPodcastChannelLongClick(bundle: Bundle?) {
        // Navigation.findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle);
    }

    companion object {
        private const val TAG = "PodcastChannelCatalogue"
    }
}