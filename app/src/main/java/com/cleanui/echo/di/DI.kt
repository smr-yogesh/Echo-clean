package com.cleanui.echo.di

import com.cleanui.echo.download.DownloadWorker
import com.cleanui.echo.download.Downloader
import com.cleanui.echo.download.db.DownloadDatabase
import com.cleanui.echo.extensions.ExtensionLoader
import com.cleanui.echo.playback.PlayerService
import com.cleanui.echo.playback.PlayerState
import com.cleanui.echo.ui.common.SnackBarHandler
import com.cleanui.echo.ui.common.UiViewModel
import com.cleanui.echo.ui.download.DownloadViewModel
import com.cleanui.echo.ui.extensions.ExtensionInfoViewModel
import com.cleanui.echo.ui.extensions.ExtensionsViewModel
import com.cleanui.echo.ui.extensions.add.AddViewModel
import com.cleanui.echo.ui.extensions.login.LoginUserListViewModel
import com.cleanui.echo.ui.extensions.login.LoginViewModel
import com.cleanui.echo.ui.feed.FeedViewModel
import com.cleanui.echo.ui.main.search.SearchViewModel
import com.cleanui.echo.ui.media.MediaViewModel
import com.cleanui.echo.ui.player.PlayerViewModel
import com.cleanui.echo.ui.player.more.info.TrackInfoViewModel
import com.cleanui.echo.ui.player.more.lyrics.LyricsViewModel
import com.cleanui.echo.ui.playlist.create.CreatePlaylistViewModel
import com.cleanui.echo.ui.playlist.delete.DeletePlaylistViewModel
import com.cleanui.echo.ui.playlist.edit.EditPlaylistViewModel
import com.cleanui.echo.ui.playlist.save.SaveToPlaylistViewModel
import com.cleanui.echo.utils.ContextUtils.getSettings
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

object DI {

    private val baseModule = module {
        single { androidApplication().getSettings() }
        singleOf(::App)
    }

    private val extensionModule = module {
        includes(baseModule)
        singleOf(::ExtensionLoader)
    }

    private val downloadModule = module {
        includes(extensionModule)
        singleOf(DownloadDatabase::create)
        singleOf(::Downloader)
        workerOf(::DownloadWorker)
    }

    private val playerModule = module {
        includes(extensionModule)
        singleOf(PlayerService::getCache)
        single { PlayerState() }
    }

    private val uiModules = module {
        singleOf(::SnackBarHandler)
        viewModelOf(::UiViewModel)

        viewModelOf(::PlayerViewModel)
        viewModelOf(::LyricsViewModel)
        viewModelOf(::TrackInfoViewModel)

        viewModelOf(::ExtensionsViewModel)
        viewModelOf(::ExtensionInfoViewModel)
        viewModelOf(::LoginUserListViewModel)
        viewModelOf(::AddViewModel)
        viewModelOf(::LoginViewModel)

        viewModelOf(::FeedViewModel)
        viewModelOf(::SearchViewModel)
        viewModelOf(::MediaViewModel)

        viewModelOf(::CreatePlaylistViewModel)
        viewModelOf(::DeletePlaylistViewModel)
        viewModelOf(::SaveToPlaylistViewModel)
        viewModelOf(::EditPlaylistViewModel)

        viewModelOf(::DownloadViewModel)
    }

    val appModule = module {
        includes(baseModule)
        includes(extensionModule)
        includes(playerModule)
        includes(downloadModule)
        includes(uiModules)
    }
}