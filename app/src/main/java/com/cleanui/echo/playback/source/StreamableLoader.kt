package com.cleanui.echo.playback.source

import android.net.Uri
import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Source.Companion.toSource
import dev.brahmkshatriya.echo.common.models.Track
import com.cleanui.echo.di.App
import com.cleanui.echo.download.Downloader
import com.cleanui.echo.extensions.ExtensionUtils.getExtensionOrThrow
import com.cleanui.echo.extensions.MediaState
import com.cleanui.echo.extensions.cache.Cached
import com.cleanui.echo.extensions.cache.Cached.loadStreamableMedia
import com.cleanui.echo.playback.MediaItemUtils
import com.cleanui.echo.playback.MediaItemUtils.backgroundIndex
import com.cleanui.echo.playback.MediaItemUtils.downloaded
import com.cleanui.echo.playback.MediaItemUtils.extensionId
import com.cleanui.echo.playback.MediaItemUtils.isLoaded
import com.cleanui.echo.playback.MediaItemUtils.serverIndex
import com.cleanui.echo.playback.MediaItemUtils.state
import com.cleanui.echo.playback.MediaItemUtils.subtitleIndex
import com.cleanui.echo.playback.MediaItemUtils.track
import com.cleanui.echo.ui.media.MediaHeaderAdapter.Companion.playableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class StreamableLoader(
    private val app: App,
    private val extensionListFlow: StateFlow<List<MusicExtension>>,
    private val downloadFlow: StateFlow<List<Downloader.Info>>
) {
    suspend fun load(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        extensionListFlow.first { it.isNotEmpty() }
        val new = if (mediaItem.isLoaded) mediaItem
        else MediaItemUtils.buildLoaded(
            app, downloadFlow.value, mediaItem, loadTrack(mediaItem)
        )

        val server = async { loadServer(new) }
        val background =
            async { if (new.backgroundIndex < 0) null else loadBackground(new).getOrNull() }
        val subtitle = async { if (new.subtitleIndex < 0) null else loadSubtitle(new).getOrNull() }

        MediaItemUtils.buildWithBackgroundAndSubtitle(
            new, background.await(), subtitle.await()
        ) to server.await()
    }

    private suspend fun <T> withClient(
        mediaItem: MediaItem,
        block: suspend (Extension<*>) -> Result<T>
    ): Result<T> {
        val extension = extensionListFlow.getExtensionOrThrow(mediaItem.extensionId)
        return block(extension)
    }

    private suspend fun loadTrack(item: MediaItem): MediaState.Loaded<Track> {
        val track = withClient(item) {
            Cached.loadMedia(app, it, item.state)
        }
        return track.getOrThrow()
    }

    private suspend fun loadServer(mediaItem: MediaItem): Result<Streamable.Media.Server> {
        val downloaded = mediaItem.downloaded
        val servers = mediaItem.track.servers
        val index = mediaItem.serverIndex
        if (!downloaded.isNullOrEmpty() && servers.size == index) {
            return runCatching {
                Streamable.Media.Server(
                    downloaded.map { Uri.fromFile(File(it)).toString().toSource() },
                    true
                )
            }
        }
        return withClient(mediaItem) {
            runCatching {
                val isPlayable = mediaItem.track.playableString(app.context)
                if (isPlayable != null) throw Exception(isPlayable)
                val streamable = servers.getOrNull(index) ?: throw Exception("Server not found")
                loadStreamableMedia(
                    app, it, mediaItem.track, streamable
                ).getOrThrow() as Streamable.Media.Server
            }
        }
    }

    private suspend fun loadBackground(mediaItem: MediaItem): Result<Streamable.Media.Background> {
        val streams = mediaItem.track.backgrounds
        val index = mediaItem.backgroundIndex
        val streamable = streams[index]
        return withClient(mediaItem) {
            runCatching {
                loadStreamableMedia(
                    app, it, mediaItem.track, streamable
                ).getOrThrow() as Streamable.Media.Background
            }
        }
    }

    private suspend fun loadSubtitle(mediaItem: MediaItem): Result<Streamable.Media.Subtitle> {
        val streams = mediaItem.track.subtitles
        val index = mediaItem.subtitleIndex
        val streamable = streams[index]
        return withClient(mediaItem) {
            runCatching {
                loadStreamableMedia(
                    app, it, mediaItem.track, streamable
                ).getOrThrow() as Streamable.Media.Subtitle
            }
        }
    }
}