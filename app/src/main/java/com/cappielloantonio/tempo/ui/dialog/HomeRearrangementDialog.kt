package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogHomeRearrangementBinding
import com.cappielloantonio.tempo.ui.adapter.HomeSectorHorizontalAdapter
import com.cappielloantonio.tempo.viewmodel.HomeRearrangementViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Collections
import java.util.Objects

class HomeRearrangementDialog : DialogFragment() {
    private var bind: DialogHomeRearrangementBinding? = null
    private var homeRearrangementViewModel: HomeRearrangementViewModel? = null
    private var homeSectorHorizontalAdapter: HomeSectorHorizontalAdapter? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogHomeRearrangementBinding.inflate(getLayoutInflater())

        homeRearrangementViewModel =
            ViewModelProvider(requireActivity()).get<HomeRearrangementViewModel>(
                HomeRearrangementViewModel::class.java
            )

        return MaterialAlertDialogBuilder(requireContext())
            .setView(bind!!.getRoot())
            .setTitle(R.string.home_rearrangement_dialog_title)
            .setPositiveButton(
                R.string.home_rearrangement_dialog_positive_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> })
            .setNeutralButton(
                R.string.home_rearrangement_dialog_neutral_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> })
            .setNegativeButton(
                R.string.home_rearrangement_dialog_negative_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })
            .create()
    }

    override fun onStart() {
        super.onStart()

        setButtonAction()
        initSectorView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        homeRearrangementViewModel!!.closeDialog()
        bind = null
    }

    private fun setButtonAction() {
        val alertDialog = Objects.requireNonNull<Dialog?>(getDialog()) as AlertDialog

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(View.OnClickListener { v: View? ->
                homeRearrangementViewModel!!.saveHomeSectorList(homeSectorHorizontalAdapter!!.items)
                dismiss()
            })

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setOnClickListener(View.OnClickListener { v: View? ->
                homeRearrangementViewModel!!.resetHomeSectorList()
                dismiss()
            })
    }

    private fun initSectorView() {
        bind!!.homeSectorItemRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.homeSectorItemRecyclerView.setHasFixedSize(true)

        homeSectorHorizontalAdapter = HomeSectorHorizontalAdapter()
        bind!!.homeSectorItemRecyclerView.setAdapter(homeSectorHorizontalAdapter)
        homeSectorHorizontalAdapter!!.items = homeRearrangementViewModel!!.homeSectorList!!

        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            var originalPosition: Int = -1
            var fromPosition: Int = -1
            var toPosition: Int = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (originalPosition == -1) originalPosition =
                    viewHolder.getBindingAdapterPosition()

                fromPosition = viewHolder.getBindingAdapterPosition()
                toPosition = target.getBindingAdapterPosition()

                Collections.swap(homeSectorHorizontalAdapter!!.items, fromPosition, toPosition)
                requireNotNull(recyclerView.adapter)
                    .notifyItemMoved(fromPosition, toPosition)

                return false
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)

                homeRearrangementViewModel!!.orderSectorLiveListAfterSwap(
                    homeSectorHorizontalAdapter!!.items
                )

                originalPosition = -1
                fromPosition = -1
                toPosition = -1
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }
        }
        ).attachToRecyclerView(bind!!.homeSectorItemRecyclerView)
    }
}
