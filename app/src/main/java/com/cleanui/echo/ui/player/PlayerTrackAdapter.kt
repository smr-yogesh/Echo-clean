package com.cleanui.echo.ui.player

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.cleanui.echo.R
import com.cleanui.echo.databinding.ItemClickPanelsBinding
import com.cleanui.echo.databinding.ItemPlayerTrackBinding
import com.cleanui.echo.playback.MediaItemUtils.track
import com.cleanui.echo.playback.MediaItemUtils.unloadedCover
import com.cleanui.echo.playback.PlayerState
import com.cleanui.echo.ui.common.UiViewModel
import com.cleanui.echo.ui.common.UiViewModel.Companion.applyHorizontalInsets
import com.cleanui.echo.ui.common.UiViewModel.Companion.applyInsets
import com.cleanui.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import com.cleanui.echo.utils.image.ImageUtils.getCachedDrawable
import com.cleanui.echo.utils.image.ImageUtils.loadWithThumb
import com.cleanui.echo.utils.ui.GestureListener
import com.cleanui.echo.utils.ui.GestureListener.Companion.handleGestures
import com.cleanui.echo.utils.ui.UiUtils.dpToPx
import com.cleanui.echo.utils.ui.UiUtils.isLandscape
import com.cleanui.echo.utils.ui.UiUtils.isRTL
import com.cleanui.echo.utils.ui.scrolling.ScrollAnimViewHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.max

class PlayerTrackAdapter(
    private val uiViewModel: UiViewModel,
    private val current: MutableStateFlow<PlayerState.Current?>,
    private val listener: Listener
) : ListAdapter<MediaItem, PlayerTrackAdapter.ViewHolder>(DiffCallback) {

    interface Listener {
        fun onClick()
        fun onLongClick() {}
        fun onStartDoubleClick() {}
        fun onEndDoubleClick() {}
    }

    object DiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.mediaId == newItem.mediaId

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemPlayerTrackBinding
    ) : ScrollAnimViewHolder(binding.root) {

        private val context = binding.root.context

        private val collapsedPadding = 8.dpToPx(context)
        private val targetZ = collapsedPadding.toFloat()
        private val size = binding.root.resources.getDimension(R.dimen.collapsed_cover_size).toInt()
        private var targetScale = 0f
        private var targetX = 0
        private var targetY = 0

        private val cover = binding.playerTrackCoverContainer
        private var currentCoverHeight = size
        private var currCoverRound = 0f
        private val isLandscape = context.isLandscape()
        fun updateCollapsed() = uiViewModel.run {
            val insets = if (!isLandscape) systemInsets.value else getCombined()
            val targetPosX = collapsedPadding + if (context.isRTL()) insets.end else insets.start
            val targetPosY = if (playerSheetState.value != STATE_EXPANDED) 0
            else collapsedPadding + systemInsets.value.top
            targetX = targetPosX - cover.left
            targetY = targetPosY - cover.top
            currentCoverHeight = cover.height.takeIf { it > 0 } ?: currentCoverHeight
            targetScale = size.toFloat() / currentCoverHeight

            val (collapsedY, offset) = if (playerSheetState.value == STATE_EXPANDED)
                systemInsets.value.top to if (isLandscape) 0f else moreSheetOffset.value
            else -collapsedPadding to 1 - max(0f, playerSheetOffset.value)

            val inv = 1 - offset
            binding.playerCollapsed.root.run {
                translationY = collapsedY - size * inv * 2
                alpha = offset
            }
            if (isLandscape) binding.clickPanel.root.scaleX = 0.5f + 0.5f * inv
            val extraY = if (!isPlayerVisible) 0f else {
                val toMoveY = binding.playerControlsPlaceholder.top - cover.top
                toMoveY * inv
            }
            val extraX = if (!isPlayerVisible) 0f else {
                val toMoveX = binding.playerControlsPlaceholder.left - cover.left
                toMoveX * inv
            }
            cover.run {
                scaleX = if (!isPlayerVisible) 1 + (targetScale - 1) * offset else targetScale
                scaleY = scaleX
                translationX = targetX * offset + extraX
                translationY = targetY * offset + extraY
                translationZ = targetZ * (1 - offset)
                currCoverRound = currentCoverHeight / 2f // vinyl: always a circle
                invalidateOutline()
            }
        }

        fun updateInsets() = uiViewModel.run {
            val (v, h) = if (!isLandscape) 64 to 0 else 0 to 24
            binding.constraintLayout.applyInsets(systemInsets.value, v, h)
            val insets = if (isLandscape) getCombined() else systemInsets.value
            binding.playerCollapsed.root.applyHorizontalInsets(insets)
            binding.playerControlsPlaceholder.run {
                updateLayoutParams {
                    height = playerControlsHeight.value
                }
                doOnLayout {
                    updateCollapsed()
                    cover.doOnLayout { updateCollapsed() }
                }
            }

            updateCollapsed()
        }

        fun updateColors() {
            binding.playerCollapsed.run {
                val colors = uiViewModel.playerColors.value ?: context.defaultPlayerColors()
                collapsedTrackTitle.setTextColor(colors.onBackground)
                collapsedTrackArtist.setTextColor(colors.onBackground)
            }
        }

        private val spin = binding.playerTrackSpin
        private var spinAnimator: ObjectAnimator? = null

        /** Slowly rotates the record while playing; pausing keeps the current angle. */
        fun setSpinning(spinning: Boolean) {
            if (spinning) {
                val animator = spinAnimator ?: ObjectAnimator.ofFloat(spin, View.ROTATION, spin.rotation, spin.rotation + 360f)
                    .apply {
                        duration = 16000L
                        interpolator = LinearInterpolator()
                        repeatCount = ValueAnimator.INFINITE
                        spinAnimator = this
                    }
                if (!animator.isStarted) animator.start() else animator.resume()
            } else spinAnimator?.takeIf { it.isStarted }?.pause()
        }

        fun releaseSpin() {
            spinAnimator?.cancel()
            spinAnimator = null
        }

        private var coverDrawable: Drawable? = null
        fun applyDrawable() {
            val index = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
            val item = getItem(index) ?: return
            val curr = current.value?.mediaItem
            if (curr != item) return
            val drawable = coverDrawable
            currentDrawableListener?.invoke(drawable)
        }

        fun bind(item: MediaItem?) {
            binding.playerCollapsed.run {
                val title = item?.track?.title
                val artist = item?.track?.artists?.joinToString(", ") { it.name }
                collapsedTrackTitle.text = title
                collapsedTrackTitle.isVisible = !title.isNullOrBlank()
                collapsedTrackArtist.text = artist
                collapsedTrackArtist.isVisible = !artist.isNullOrBlank()
            }
            val old = item?.unloadedCover?.getCachedDrawable(binding.root.context)
            item?.track?.cover.loadWithThumb(binding.playerTrackCover, old) {
                val image = it
                    ?: ResourcesCompat.getDrawable(resources, R.drawable.art_music, context.theme)
                setImageDrawable(image)
                coverDrawable = it
                applyDrawable()
            }
            updateInsets()
            updateColors()
        }

        init {
            cover.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(
                        0, 0, currentCoverHeight, currentCoverHeight, currCoverRound
                    )
                }
            }
            cover.clipToOutline = true
            cover.doOnLayout { updateInsets() }
            binding.clickPanel.configureClicking(listener, uiViewModel)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPlayerTrackBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    private var spinning = false
    fun setSpinning(value: Boolean) {
        spinning = value
        onEachViewHolder { setSpinning(value) }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.setSpinning(spinning)
        holder.updateInsets()
        holder.updateColors()
        holder.applyDrawable()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.releaseSpin()
        holder.updateInsets()
        holder.updateColors()
        holder.applyDrawable()
    }

    private fun onEachViewHolder(block: ViewHolder.() -> Unit) {
        val recyclerView = recyclerView ?: return
        recyclerView.run {
            for (it in 0 until childCount) {
                val viewHolder = getChildViewHolder(getChildAt(it)) as? ViewHolder
                viewHolder?.block()
            }
        }
    }

    fun moreOffsetUpdated() = onEachViewHolder { updateCollapsed() }
    fun playerOffsetUpdated() = onEachViewHolder { updateCollapsed() }
    fun playerSheetStateUpdated() = onEachViewHolder { updateInsets() }
    fun insetsUpdated() = onEachViewHolder { updateInsets() }
    fun playerControlsHeightUpdated() = onEachViewHolder { updateInsets() }
    fun onColorsUpdated() = onEachViewHolder { updateColors() }
    fun onCurrentUpdated() {
        onEachViewHolder { applyDrawable() }
        if (current.value == null) currentDrawableListener?.invoke(null)
    }

    private var isPlayerVisible = false
    fun updatePlayerVisibility(visible: Boolean) {
        isPlayerVisible = visible
        onEachViewHolder { updateInsets() }
    }

    var currentDrawableListener: ((Drawable?) -> Unit)? = null

    companion object {
        fun ItemClickPanelsBinding.configureClicking(listener: Listener, uiViewModel: UiViewModel) {
            start.handleGestures(object : GestureListener {
                override val onClick = listener::onClick
                override val onLongClick = listener::onLongClick
                override val onDoubleClick: (() -> Unit)?
                    get() = if (uiViewModel.playerSheetState.value != STATE_EXPANDED) null
                    else listener::onStartDoubleClick
            })
            end.handleGestures(object : GestureListener {
                override val onClick = listener::onClick
                override val onLongClick = listener::onLongClick
                override val onDoubleClick: (() -> Unit)?
                    get() = if (uiViewModel.playerSheetState.value != STATE_EXPANDED) null
                    else listener::onEndDoubleClick
            })
        }
    }
}