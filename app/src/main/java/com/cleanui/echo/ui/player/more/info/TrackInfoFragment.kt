package com.cleanui.echo.ui.player.more.info

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import com.cleanui.echo.R
import com.cleanui.echo.ui.media.MediaDetailsFragment
import com.cleanui.echo.utils.ui.AnimationUtils.setupTransition
import org.koin.androidx.viewmodel.ext.android.viewModel

class TrackInfoFragment : Fragment(
    R.layout.fragment_player_info
), MediaDetailsFragment.Parent {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view, axis = MaterialSharedAxis.Y)
    }

    override val feedId = "player"
    override val fromPlayer = true
    override val viewModel by viewModel<TrackInfoViewModel>()
}