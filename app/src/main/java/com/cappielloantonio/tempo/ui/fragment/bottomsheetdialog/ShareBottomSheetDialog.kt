package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.ui.dialog.ShareUpdateDialog
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.UIUtil.getReadableDate
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import com.cappielloantonio.tempo.viewmodel.ShareBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@UnstableApi
class ShareBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private var homeViewModel: HomeViewModel? = null
    private var shareBottomSheetViewModel: ShareBottomSheetViewModel? = null
    private var share: Share? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_share_dialog, container, false)

        share = BundleCompat.getParcelable(
            requireArguments(),
            Constants.SHARE_OBJECT,
            Share::class.java
        )

        homeViewModel =
            ViewModelProvider(requireActivity()).get<HomeViewModel>(HomeViewModel::class.java)
        shareBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<ShareBottomSheetViewModel>(
                ShareBottomSheetViewModel::class.java
            )
        shareBottomSheetViewModel!!.setShare(share!!)

        init(view)

        return view
    }

    private fun init(view: View) {
        val shareCover = view.findViewById<ImageView>(R.id.share_cover_image_view)

        var coverArtId: String? = null
        val entries: MutableList<Child?>? = shareBottomSheetViewModel!!.getShare().entries?.toMutableList()

        if (entries != null && !entries.isEmpty()) {
            coverArtId = entries.get(0)!!.coverArtId
        }

        from(requireContext(), coverArtId, CustomGlideRequest.ResourceType.Unknown)
            .build()
            .into(shareCover)

        val shareTitle = view.findViewById<TextView>(R.id.share_title_text_view)
        shareTitle.setText(shareBottomSheetViewModel!!.getShare().description)
        shareTitle.setSelected(true)

        val shareSubtitle = view.findViewById<TextView>(R.id.share_subtitle_text_view)
        shareSubtitle.setText(
            requireContext().getString(
                R.string.share_subtitle_item,
                getReadableDate(share!!.expires)
            )
        )
        shareSubtitle.setSelected(true)

        val copyLink = view.findViewById<TextView>(R.id.copy_link_text_view)
        copyLink.setOnClickListener(View.OnClickListener { v: View? ->
            val clipboardManager =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(
                getString(R.string.app_name),
                shareBottomSheetViewModel!!.getShare().url
            )
            clipboardManager.setPrimaryClip(clipData)
            dismissBottomSheet()
        })

        val updateShare = view.findViewById<TextView>(R.id.update_share_preferences_text_view)
        updateShare.setOnClickListener(View.OnClickListener { v: View? ->
            // refreshShares();
            showUpdateShareDialog()
            dismissBottomSheet()
        })

        val deleteShare = view.findViewById<TextView>(R.id.delete_share_text_view)
        deleteShare.setOnClickListener(View.OnClickListener { v: View? ->
            deleteShare()
            refreshShares()
            dismissBottomSheet()
        })
    }

    override fun onClick(v: View?) {
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

    private fun showUpdateShareDialog() {
        val dialog = ShareUpdateDialog()
        dialog.show(requireActivity().getSupportFragmentManager(), null)
    }

    private fun refreshShares() {
        homeViewModel!!.refreshShares(getParentFragment()!!)
    }

    private fun deleteShare() {
        shareBottomSheetViewModel!!.deleteShare()
    }
}