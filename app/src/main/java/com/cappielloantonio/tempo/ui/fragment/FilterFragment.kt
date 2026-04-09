package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentFilterBinding
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.FilterViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.chip.Chip

@OptIn(markerClass = [UnstableApi::class])
class FilterFragment : Fragment() {
    private var activity: MainActivity? = null
    private var bind: FragmentFilterBinding? = null
    private var filterViewModel: FilterViewModel? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = getActivity() as MainActivity?
        bind = FragmentFilterBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        filterViewModel =
            ViewModelProvider(requireActivity()).get<FilterViewModel>(FilterViewModel::class.java)

        init()
        initAppBar()
        setFilterChips()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun init() {
        val bundle = Bundle()
        bundle.putString(Constants.MEDIA_BY_GENRES, Constants.MEDIA_BY_GENRES)
        bundle.putStringArrayList("filters_list", filterViewModel!!.filters)
        bundle.putStringArrayList("filter_name_list", filterViewModel!!.filterNames)
        bind!!.finishFilteringTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            if (filterViewModel!!.filters.size > 1) activity!!.navController!!.navigate(
                R.id.action_filterFragment_to_songListPageFragment,
                bundle
            )
            else Toast.makeText(
                requireContext(),
                getString(R.string.filter_info_selection),
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    private fun initAppBar() {
        activity!!.setSupportActionBar(bind!!.toolbar)

        if (activity!!.getSupportActionBar() != null) {
            activity!!.getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
            activity!!.getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        }

        bind!!.toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> activity!!.navController!!.navigateUp() })


        bind!!.appBarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout: AppBarLayout?, verticalOffset: Int ->
            if ((bind!!.genreFilterInfoSector.getHeight() + verticalOffset) < (2 * ViewCompat.getMinimumHeight(
                    bind!!.toolbar
                ))
            ) {
                bind!!.toolbar.setTitle(R.string.filter_title)
            } else {
                bind!!.toolbar.setTitle(R.string.empty_string)
            }
        })
    }

    private fun setFilterChips() {
        filterViewModel!!.genreList.observe(
            getViewLifecycleOwner(),
            Observer { genres: MutableList<Genre?>? ->
                bind!!.loadingProgressBar.setVisibility(View.GONE)
                bind!!.filterContainer.setVisibility(View.VISIBLE)
                for (genre in genres!!) {
                    val chip = requireActivity().getLayoutInflater()
                        .inflate(R.layout.chip_search_filter_genre, null, false) as Chip
                    chip.setText(genre!!.genre)
                    chip.setChecked(filterViewModel!!.filters.contains(genre.genre))
                    chip.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                        if (isChecked) filterViewModel!!.addFilter(
                            genre.genre,
                            buttonView!!.getText().toString()
                        )
                        else filterViewModel!!.removeFilter(
                            genre.genre,
                            buttonView!!.getText().toString()
                        )
                    })
                    bind!!.filtersChipsGroup.addView(chip)
                }
            })
    }

    companion object {
        private const val TAG = "FilterFragment"
    }
}
