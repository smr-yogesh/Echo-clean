package com.cleanui.echo.ui.feed.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.cleanui.echo.databinding.ItemShelfHeaderBinding
import com.cleanui.echo.ui.feed.FeedClickListener
import com.cleanui.echo.ui.feed.FeedType

class HeaderViewHolder(
    parent: ViewGroup,
    listener: FeedClickListener,
    private val binding: ItemShelfHeaderBinding = ItemShelfHeaderBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )
) : FeedViewHolder<FeedType.Header>(binding.root) {

    private var feed: FeedType.Header? = null

    private val baseMargins = (binding.root.layoutParams as? ViewGroup.MarginLayoutParams)
        ?.let { it.topMargin to it.bottomMargin } ?: (0 to 0)

    init {
        binding.more.setOnClickListener {
            listener.openFeed(
                it,
                feed?.extensionId,
                feed?.id,
                feed?.title,
                feed?.subtitle,
                feed?.more
            )
        }
        binding.shuffle.setOnClickListener {
            listener.onPlayClicked(it, feed?.extensionId, feed?.context, feed?.tracks, true)
        }
    }

    override fun bind(feed: FeedType.Header) = bind(feed, false)

    /**
     * @param hideTitle drop the title text (used on Home when the extension supplies its own
     * greeting, so only the app's greeting header is shown).
     * A header with nothing left to show takes no space at all.
     */
    fun bind(feed: FeedType.Header, hideTitle: Boolean) {
        this.feed = feed
        val titleText = if (hideTitle) "" else feed.title
        binding.title.text = titleText
        binding.title.isVisible = titleText.isNotBlank()
        binding.subtitle.text = feed.subtitle
        binding.subtitle.isVisible = !feed.subtitle.isNullOrBlank()
        binding.shuffle.isVisible = feed.tracks != null
        binding.more.isVisible = feed.more != null
        val empty = titleText.isBlank() && feed.subtitle.isNullOrBlank()
                && feed.tracks == null && feed.more == null
        setCollapsed(empty)
    }

    private fun setCollapsed(collapsed: Boolean) {
        val params = binding.root.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        params.height = if (collapsed) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
        params.topMargin = if (collapsed) 0 else baseMargins.first
        params.bottomMargin = if (collapsed) 0 else baseMargins.second
        binding.root.layoutParams = params
        binding.root.isVisible = !collapsed
    }
}