
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
import com.cappielloantonio.tempo.databinding.FragmentGenreCatalogueBinding
import com.cappielloantonio.tempo.helper.recyclerview.GridItemDecoration
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.GenreCatalogueAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.TileSizeManager.Companion.instance
import com.cappielloantonio.tempo.viewmodel.GenreCatalogueViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener

@OptIn(markerClass = [UnstableApi::class])
class GenreCatalogueFragment : Fragment(), ClickCallback {
    private var bind: FragmentGenreCatalogueBinding? = null
    private var activity: MainActivity? = null
    private var genreCatalogueViewModel: GenreCatalogueViewModel? = null

    private var genreCatalogueAdapter: GenreCatalogueAdapter? = null

    private var spanCount = 2
    private var tileSpacing = 20

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

        bind = FragmentGenreCatalogueBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        genreCatalogueViewModel = ViewModelProvider(requireActivity()).get<GenreCatalogueViewModel>(
            GenreCatalogueViewModel::class.java
        )

        instance!!.calculateGenreSize(requireContext())
        spanCount = instance!!.getGenreSpanCount(requireContext())
        tileSpacing = instance!!.getGenreSpacing(requireContext())

        init()
        initAppBar()
        initGenreCatalogueView()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun init() {
        bind!!.filterGenresTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            activity!!.navController!!.navigate(
                R.id.action_genreCatalogueFragment_to_filterFragment
            )
        })
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
            if ((bind!!.genreInfoSector.getHeight() + verticalOffset) < (2 * bind!!.toolbar.minimumHeight)
            ) {
                bind!!.toolbar.setTitle(R.string.genre_catalogue_title)
            } else {
                bind!!.toolbar.setTitle(R.string.empty_string)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initGenreCatalogueView() {
        bind!!.genreCatalogueRecyclerView.setLayoutManager(
            GridLayoutManager(
                requireContext(),
                spanCount
            )
        )
        bind!!.genreCatalogueRecyclerView.addItemDecoration(
            GridItemDecoration(
                spanCount,
                tileSpacing,
                false
            )
        )
        bind!!.genreCatalogueRecyclerView.setHasFixedSize(true)

        genreCatalogueAdapter = GenreCatalogueAdapter(this)
        genreCatalogueAdapter!!.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY)
        bind!!.genreCatalogueRecyclerView.setAdapter(genreCatalogueAdapter)

        genreCatalogueViewModel!!.genreList.observe(
            getViewLifecycleOwner(),
            Observer { genres: MutableList<Genre?>? -> genreCatalogueAdapter!!.setItems(genres!!) })

        bind!!.genreCatalogueRecyclerView.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            hideKeyboard(v!!)
            false
        })

        bind!!.genreListSortImageView.setOnClickListener(View.OnClickListener { view: View? ->
            showPopupMenu(
                view,
                R.menu.sort_genre_popup_menu
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
                genreCatalogueAdapter!!.getFilter().filter(newText)
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

        popup.setOnMenuItemClickListener { menuItem ->
            if (menuItem!!.itemId == R.id.menu_genre_sort_name) {
                genreCatalogueAdapter!!.sort(Constants.GENRE_ORDER_BY_NAME)
                return@setOnMenuItemClickListener true
            } else if (menuItem.itemId == R.id.menu_genre_sort_random) {
                genreCatalogueAdapter!!.sort(Constants.GENRE_ORDER_BY_RANDOM)
                return@setOnMenuItemClickListener true
            }
            false
        }

        popup.show()
    }

    override fun onGenreClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.songListPageFragment, bundle)
        hideKeyboard(requireView())
    }
}