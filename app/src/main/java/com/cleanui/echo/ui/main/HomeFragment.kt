package com.cleanui.echo.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import com.cleanui.echo.R
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Buttons.Companion.EMPTY
import dev.brahmkshatriya.echo.common.models.Shelf
import com.cleanui.echo.databinding.FragmentHomeBinding
import com.cleanui.echo.extensions.ExtensionUtils.getAs
import com.cleanui.echo.extensions.cache.Cached
import com.cleanui.echo.ui.common.GridAdapter.Companion.configureGridLayout
import com.cleanui.echo.ui.common.UiViewModel
import com.cleanui.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import com.cleanui.echo.ui.common.UiViewModel.Companion.configure
import com.cleanui.echo.ui.extensions.list.ExtensionsListBottomSheet
import com.cleanui.echo.ui.feed.FeedAdapter.Companion.getFeedAdapter
import com.cleanui.echo.ui.feed.FeedAdapter.Companion.getTouchHelper
import com.cleanui.echo.ui.feed.FeedClickListener.Companion.getFeedListener
import com.cleanui.echo.ui.feed.FeedData
import com.cleanui.echo.ui.feed.FeedViewModel
import com.cleanui.echo.ui.main.MainFragment.Companion.applyInsets
import com.cleanui.echo.utils.ContextUtils.observe
import com.cleanui.echo.utils.ui.AnimationUtils.setupTransition
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val feedData by lazy {
        val vm by viewModel<FeedViewModel>()
        val id = "home"
        vm.getFeedData(id, EMPTY, cached = {
            val curr = current.value!!
            val feed = Cached.getFeedShelf(app, curr.id, id).getOrThrow()
            FeedData.State(curr.id, null, feed)
        }) {
            val curr = current.value!!
            val feed = Cached.savingFeed(
                app, curr, id,
                curr.getAs<HomeFeedClient, Feed<Shelf>> { loadHomeFeed() }.getOrThrow()
            )
            FeedData.State(curr.id, null, feed)
        }
    }

    private val listener by lazy { getFeedListener(requireParentFragment()) }
    private val feedAdapter by lazy { getFeedAdapter(feedData, listener, quickAccess = true) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHomeBinding.bind(view)
        setupTransition(view, false, MaterialSharedAxis.Y)
        applyInsets(binding.recyclerView, binding.appBarOutline) {
            binding.swipeRefresh.configure(it)
        }
        val uiViewModel by activityViewModel<UiViewModel>()
        observe(uiViewModel.navigationReselected) {
            if (it != 0) return@observe
            ExtensionsListBottomSheet.newInstance(ExtensionType.MUSIC)
                .show(parentFragmentManager, null)
        }
        observe(
            uiViewModel.navigation.combine(feedData.backgroundImageFlow) { a, b -> a to b }
        ) { (curr, bg) ->
            if (curr != 0) return@observe
            uiViewModel.currentNavBackground.value = bg
        }
        applyBackPressCallback()
        getTouchHelper(listener).attachToRecyclerView(binding.recyclerView)
        configureGridLayout(
            binding.recyclerView,
            feedAdapter.withLoading(this, HeaderAdapter(this) { HeaderAdapter.greeting(it) })
        )
        binding.swipeRefresh.run {
            setOnRefreshListener { feedData.refresh() }
            observe(feedData.isRefreshingFlow) {
                isRefreshing = it
            }
        }
    }
}