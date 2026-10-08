package com.cleanui.echo.ui.feed.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import com.cleanui.echo.databinding.ItemShelfListsBinding
import com.cleanui.echo.playback.PlayerState
import com.cleanui.echo.ui.feed.FeedClickListener
import com.cleanui.echo.ui.feed.FeedType
import com.cleanui.echo.ui.feed.viewholders.shelf.ShelfType
import com.cleanui.echo.ui.feed.viewholders.shelf.ShelfViewHolder
import com.cleanui.echo.utils.ui.AnimationUtils.applyTranslationAndScaleAnimation
import com.cleanui.echo.utils.ui.UiUtils.dpToPx

class HorizontalListViewHolder(
    parent: ViewGroup,
    listener: FeedClickListener,
    pool: RecyclerView.RecycledViewPool,
    private val binding: ItemShelfListsBinding = ItemShelfListsBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )
) : FeedViewHolder<FeedType.HorizontalList>(binding.root) {
    val adapter = Adapter(listener)
    private val railLayoutManager =
        LinearLayoutManager(parent.context, RecyclerView.HORIZONTAL, false)
    private val quickLayoutManager = GridLayoutManager(parent.context, QUICK_SPAN)

    /** The layout manager currently attached (rail or quick-access grid). */
    val layoutManager: LinearLayoutManager
        get() = binding.root.layoutManager as? LinearLayoutManager ?: railLayoutManager

    init {
        binding.root.setRecycledViewPool(pool)
        binding.root.layoutManager = railLayoutManager
    }

    override fun bind(feed: FeedType.HorizontalList) = bind(feed, false)

    /**
     * @param quickAccess render as a compact 2-column grid of small cards (Spotify-style
     * shortcuts) instead of a horizontally scrolling rail.
     */
    fun bind(feed: FeedType.HorizontalList, quickAccess: Boolean) {
        val root = binding.root
        val wanted = if (quickAccess) quickLayoutManager else railLayoutManager
        if (root.layoutManager !== wanted) root.layoutManager = wanted
        root.isNestedScrollingEnabled = !quickAccess
        adapter.quick = quickAccess

        val endPadding = if (feed.shelf is Shelf.Lists.Tracks) 8 else 20
        root.updatePaddingRelative(end = endPadding.dpToPx(root.context))
        adapter.tracks = feed.shelf.list.filterIsInstance<Track>()
        val items = feed.shelf.toShelfType(feed.extensionId, feed.context, feed.tabId)
        adapter.submitList(if (quickAccess) items.take(QUICK_MAX) else items) {
            root.adapter = adapter
        }
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        adapter.onCurrentChanged(current)
    }

    fun Shelf.Lists<*>.toShelfType(
        extensionId: String, context: EchoMediaItem?, tabId: String?
    ) = when (this) {
        is Shelf.Lists.Items -> list.map { ShelfType.Media(extensionId, context, tabId, it) }
        is Shelf.Lists.Categories -> list.map {
            ShelfType.Category(extensionId, context, tabId, it)
        }

        is Shelf.Lists.Tracks -> list.chunked(3).mapIndexed { index, it ->
            ShelfType.ThreeTracks(
                extensionId, context, tabId, index,
                Triple(
                    it[0],
                    it.getOrNull(1),
                    it.getOrNull(2)
                )
            )
        }
    }

    companion object {
        const val QUICK_SPAN = 2
        const val QUICK_MAX = 8
        const val QUICK_VIEW_TYPE = 100
    }

    object DiffCallback : DiffUtil.ItemCallback<ShelfType>() {
        override fun areItemsTheSame(oldItem: ShelfType, newItem: ShelfType): Boolean {
            if (oldItem.extensionId != newItem.extensionId) return false
            if (oldItem.type != newItem.type) return false
            if (oldItem.id != newItem.id) return false
            return true
        }

        override fun areContentsTheSame(oldItem: ShelfType, newItem: ShelfType): Boolean {
            return oldItem == newItem
        }
    }

    class Adapter(
        private val listener: FeedClickListener
    ) : ListAdapter<ShelfType, ShelfViewHolder<*>>(DiffCallback) {
        var tracks: List<Track> = emptyList()

        /** When true, [ShelfType.Media] entries use the compact quick-access card. */
        var quick = false
        override fun getItemViewType(position: Int): Int {
            val type = currentList[position].type
            return if (quick && type == ShelfType.Enum.Media) QUICK_VIEW_TYPE else type.ordinal
        }

        override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int
        ): ShelfViewHolder<*> = if (viewType == QUICK_VIEW_TYPE) ShelfViewHolder.Quick(
            parent, listener
        ) else when (ShelfType.Enum.entries[viewType]) {
            ShelfType.Enum.Category -> ShelfViewHolder.Category(parent, listener)
            ShelfType.Enum.Media -> ShelfViewHolder.Media(parent, listener)
            ShelfType.Enum.ThreeTracks -> ShelfViewHolder.ThreeTracks(
                parent, listener, { tracks }
            )
        }

        override fun onBindViewHolder(
            holder: ShelfViewHolder<*>, position: Int
        ) {
            holder.itemView.applyTranslationAndScaleAnimation(scrollAmountX)
            holder.scrollX = scrollAmountX
            when (holder) {
                is ShelfViewHolder.Category -> holder.bind(
                    position,
                    currentList.map { it as ShelfType.Category })

                is ShelfViewHolder.Media -> holder.bind(
                    position,
                    currentList.map { it as ShelfType.Media })

                is ShelfViewHolder.Quick -> holder.bind(
                    position,
                    currentList.map { it as ShelfType.Media })

                is ShelfViewHolder.ThreeTracks -> holder.bind(
                    position,
                    currentList.map { it as ShelfType.ThreeTracks })
            }
            holder.onCurrentChanged(current)
        }

        var current: PlayerState.Current? = null

        override fun onViewDetachedFromWindow(holder: ShelfViewHolder<*>) {
            holder.onCurrentChanged(current)
        }

        override fun onViewAttachedToWindow(holder: ShelfViewHolder<*>) {
            holder.onCurrentChanged(current)
        }

        private var scrollAmountX: Int = 0
        private val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                scrollAmountX = dx
            }
        }

        var recyclerView: RecyclerView? = null
        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            this.recyclerView = recyclerView
            recyclerView.addOnScrollListener(scrollListener)
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            recyclerView.removeOnScrollListener(scrollListener)
            this.recyclerView = null
        }

        private fun onEachViewHolder(action: ShelfViewHolder<*>.() -> Unit) {
            recyclerView?.let { rv ->
                for (i in 0 until rv.childCount) {
                    val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? ShelfViewHolder<*>
                    holder?.action()
                }
            }
        }

        fun onCurrentChanged(current: PlayerState.Current?) {
            this.current = current
            onEachViewHolder { onCurrentChanged(current) }
        }
    }
}