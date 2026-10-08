package com.cleanui.echo.ui.feed.viewholders

import android.view.View
import com.cleanui.echo.playback.PlayerState
import com.cleanui.echo.ui.feed.FeedType
import com.cleanui.echo.utils.ui.scrolling.ScrollAnimViewHolder

sealed class FeedViewHolder<T : FeedType>(view: View) : ScrollAnimViewHolder(view) {
    abstract fun bind(feed: T)
    open fun canBeSwiped(): Boolean = false
    open fun onSwipe(): T? = null
    open fun onCurrentChanged(current: PlayerState.Current?) {}
}