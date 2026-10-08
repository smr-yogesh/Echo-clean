package com.cleanui.echo.download.exceptions

import com.cleanui.echo.download.db.models.DownloadEntity
import com.cleanui.echo.download.db.models.TaskType

data class DownloadException(
    val type: TaskType,
    val downloadEntity: DownloadEntity,
    override val cause: Throwable
) : Exception()