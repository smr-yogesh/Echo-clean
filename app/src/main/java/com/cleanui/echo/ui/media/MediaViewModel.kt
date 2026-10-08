package com.cleanui.echo.ui.media

import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import com.cleanui.echo.di.App
import com.cleanui.echo.download.Downloader
import com.cleanui.echo.extensions.ExtensionLoader
import com.cleanui.echo.extensions.MediaState
import com.cleanui.echo.extensions.cache.Cached
import com.cleanui.echo.extensions.cache.Cached.loadMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class MediaViewModel(
    extensionLoader: ExtensionLoader,
    downloader: Downloader,
    val app: App,
    loadFeeds: Boolean,
    val extensionId: String,
    val item: EchoMediaItem,
    val loaded: Boolean,
) : MediaDetailsViewModel(
    downloader, app, loadFeeds,
    extensionLoader.music.map { list -> list.find { it.id == extensionId } }
) {

    override fun getItem(): Triple<String, EchoMediaItem, Boolean> {
        val result = itemResultFlow.value?.getOrNull()?.item
        return Triple(
            extensionId,
            result ?: item,
            loaded || result != null
        )
    }

    init {
        var force = false
        viewModelScope.launch(Dispatchers.IO) {
            listOf(extensionFlow, refreshFlow).merge().collectLatest {
                itemResultFlow.value = null
                cacheResultFlow.value = null
                cacheResultFlow.value = Cached.getMedia<EchoMediaItem>(app, extensionId, item.id)
                    .getOrNull()?.let { Result.success(it) }
                val extension = extensionFlow.value ?: return@collectLatest
                itemResultFlow.value = loadMedia(
                    app, extension, MediaState.Unloaded(extension.id, item)
                )
                force = true
            }
        }
    }
}