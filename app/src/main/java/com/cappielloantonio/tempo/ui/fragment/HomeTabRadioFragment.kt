package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.databinding.FragmentHomeTabRadioBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.interfaces.RadioCallback
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.InternetRadioStationAdapter
import com.cappielloantonio.tempo.ui.dialog.RadioEditorDialog
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences.setRadioSectionHidden
import com.cappielloantonio.tempo.viewmodel.RadioViewModel
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future

@UnstableApi
class HomeTabRadioFragment : Fragment(), ClickCallback, RadioCallback {
    private var bind: FragmentHomeTabRadioBinding? = null
    private var activity: MainActivity? = null
    private var radioViewModel: RadioViewModel? = null

    private var internetRadioStationAdapter: InternetRadioStationAdapter? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = getActivity() as MainActivity?

        bind = FragmentHomeTabRadioBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        radioViewModel =
            ViewModelProvider(requireActivity()).get<RadioViewModel>(RadioViewModel::class.java)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        initRadioStationView()
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
        bind!!.internetRadioStationPreTextView.setOnClickListener(View.OnClickListener { v: View? ->
            val dialog = RadioEditorDialog(this)
            dialog.show(activity!!.getSupportFragmentManager(), null)
        })

        bind!!.internetRadioStationTitleTextView.setOnLongClickListener(OnLongClickListener { v: View? ->
            radioViewModel!!.getInternetRadioStations(getViewLifecycleOwner())
            true
        })

        bind!!.hideSectionButton.setOnClickListener(View.OnClickListener { v: View? -> setRadioSectionHidden() })
    }

    private fun initRadioStationView() {
        bind!!.internetRadioStationRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.internetRadioStationRecyclerView.setHasFixedSize(true)

        internetRadioStationAdapter = InternetRadioStationAdapter(this)
        bind!!.internetRadioStationRecyclerView.setAdapter(internetRadioStationAdapter)
        radioViewModel!!.getInternetRadioStations(getViewLifecycleOwner()).observe(
            getViewLifecycleOwner(),
            Observer { internetRadioStations: MutableList<InternetRadioStation?>? ->
                if (internetRadioStations == null) {
                    if (bind != null) bind!!.homeRadioStationSector.setVisibility(View.GONE)
                    if (bind != null) bind!!.emptyRadioStationLayout.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeRadioStationSector.setVisibility(if (!internetRadioStations.isEmpty()) View.VISIBLE else View.GONE)
                    if (bind != null) bind!!.emptyRadioStationLayout.setVisibility(if (internetRadioStations.isEmpty()) View.VISIBLE else View.GONE)

                    internetRadioStationAdapter!!.setItems(internetRadioStations)
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
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        val raw: Any = mediaBrowserListenableFuture!!
        @Suppress("UNCHECKED_CAST")
        val future: Future<out androidx.media3.session.MediaController> = raw as Future<out androidx.media3.session.MediaController>
        MediaBrowser.releaseFuture(future)
    }

    override fun onInternetRadioStationClick(bundle: Bundle?) {
        MediaManager.startRadio(
            mediaBrowserListenableFuture, bundle?.getParcelable<InternetRadioStation>(
                Constants.INTERNET_RADIO_STATION_OBJECT
            )!!
        )
        activity!!.setBottomSheetInPeek(true)
    }

    override fun onInternetRadioStationLongClick(bundle: Bundle?) {
        val dialog = RadioEditorDialog(object : RadioCallback {
            override fun onDismiss() {
                radioViewModel!!.getInternetRadioStations(getViewLifecycleOwner())
            }
        })
        dialog.setArguments(bundle)
        dialog.show(activity!!.getSupportFragmentManager(), null)
    }

    override fun onDismiss() {
        Handler().postDelayed(Runnable {
            if (radioViewModel != null) radioViewModel!!.refreshInternetRadioStations(
                getViewLifecycleOwner()
            )
        }, 1000)
    }

    companion object {
        private const val TAG = "HomeTabRadioFragment"
    }
}
