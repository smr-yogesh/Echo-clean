package com.cleanui.echo.ui.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import com.cleanui.echo.databinding.ItemLineBinding
import com.cleanui.echo.ui.common.GridAdapter
import com.cleanui.echo.utils.ui.scrolling.ScrollAnimLoadStateAdapter
import com.cleanui.echo.utils.ui.scrolling.ScrollAnimViewHolder

class LineAdapter : ScrollAnimLoadStateAdapter<LineAdapter.ViewHolder>(), GridAdapter {
    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = count
    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState) = ViewHolder(parent)
    class ViewHolder(
        parent: ViewGroup,
        binding: ItemLineBinding = ItemLineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : ScrollAnimViewHolder(binding.root)
}