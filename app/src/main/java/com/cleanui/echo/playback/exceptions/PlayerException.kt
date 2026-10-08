package com.cleanui.echo.playback.exceptions

import androidx.media3.common.MediaItem

class PlayerException(
    val mediaItem: MediaItem?,
    override val cause: Throwable
) : Exception()