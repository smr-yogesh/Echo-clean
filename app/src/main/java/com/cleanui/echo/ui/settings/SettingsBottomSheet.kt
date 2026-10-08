package com.cleanui.echo.ui.settings

import android.graphics.drawable.Animatable
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.cleanui.echo.MainApplication.Companion.getCurrentLanguage
import com.cleanui.echo.MainApplication.Companion.languages
import com.cleanui.echo.MainApplication.Companion.setCurrentLanguage
import com.cleanui.echo.R
import dev.brahmkshatriya.echo.common.models.ExtensionType
import com.cleanui.echo.databinding.DialogSettingsBinding
import com.cleanui.echo.extensions.db.models.UserEntity.Companion.toEntity
import com.cleanui.echo.ui.common.FragmentUtils.openFragment
import com.cleanui.echo.ui.download.DownloadFragment
import com.cleanui.echo.ui.download.DownloadViewModel
import com.cleanui.echo.ui.extensions.ExtensionInfoFragment
import com.cleanui.echo.ui.extensions.ExtensionInfoFragment.Companion.openLink
import com.cleanui.echo.ui.extensions.ExtensionsViewModel
import com.cleanui.echo.ui.extensions.add.ExtensionsAddBottomSheet
import com.cleanui.echo.ui.extensions.list.ExtensionsListBottomSheet
import com.cleanui.echo.ui.extensions.login.LoginUserListBottomSheet
import com.cleanui.echo.ui.extensions.login.LoginUserListViewModel
import com.cleanui.echo.ui.extensions.manage.ManageExtensionsFragment
import com.cleanui.echo.ui.main.HeaderAdapter.Companion.loadBigIcon
import com.cleanui.echo.ui.main.HeaderAdapter.Companion.setLoopedLongClick
import com.cleanui.echo.utils.ContextUtils.appVersion
import com.cleanui.echo.utils.ContextUtils.copyToClipboard
import com.cleanui.echo.utils.ContextUtils.getArch
import com.cleanui.echo.utils.ContextUtils.getSettings
import com.cleanui.echo.utils.ContextUtils.observe
import com.cleanui.echo.utils.ui.UiUtils.configureBottomBar
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.random.Random

class SettingsBottomSheet : BottomSheetDialogFragment(R.layout.dialog_settings) {
    private val viewModel by activityViewModel<LoginUserListViewModel>()
    private val downloadVM by activityViewModel<DownloadViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = DialogSettingsBinding.bind(view)

        binding.closeButton.setOnClickListener { dismiss() }
        binding.logoImage.setOnClickListener {
            binding.logoImage.setImageResource(R.drawable.art_splash_anim)
            (binding.logoImage.drawable as Animatable).start()
            when (Random.nextInt(5)) {
                0 -> Toast.makeText(it.context, "Ooo what do we have here?", Toast.LENGTH_SHORT)
                    .show()

                2 -> Toast.makeText(it.context, "Nothing to see here.", Toast.LENGTH_SHORT).show()
            }
        }
        (binding.logoImage.drawable as Animatable).start()

        binding.player.setOnClickListener {
            dismiss()
            requireActivity().openFragment<SettingsPlayerFragment>()
        }

        binding.lookAndFeel.setOnClickListener {
            dismiss()
            requireActivity().openFragment<SettingsLookFragment>()
        }

        binding.other.setOnClickListener {
            dismiss()
            requireActivity().openFragment<SettingsOtherFragment>()
        }

        binding.downloads.setOnClickListener {
            dismiss()
            requireActivity().openFragment<DownloadFragment>()
        }

        observe(downloadVM.downloadExtensions) { extensions ->
            val extension = extensions?.firstOrNull()
            binding.downloadSettings.isEnabled = extension != null
            binding.downloadSettings.setOnClickListener {
                dismiss()
                extension ?: return@setOnClickListener
                requireActivity().openFragment<ExtensionInfoFragment>(
                    null, ExtensionInfoFragment.getBundle(extension)
                )
            }
        }

        binding.manageExtension.setOnClickListener {
            dismiss()
            requireActivity().openFragment<ManageExtensionsFragment>()
        }

        binding.addExtension.setOnClickListener {
            dismiss()
            ExtensionsAddBottomSheet().show(parentFragmentManager, null)
        }

        val settings = requireContext().getSettings()
        val language = getCurrentLanguage(settings)
        val languages = mapOf("system" to getString(R.string.system)) + languages
        val langList = languages.entries.toList()
        binding.language.run {
            text = getString(R.string.language_x, languages[language])
            setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setSingleChoiceItems(
                        langList.map { it.value }.toTypedArray(),
                        langList.indexOfFirst { it.key == language }
                    ) { dialog, which ->
                        setCurrentLanguage(settings, langList[which].key)
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .setTitle(getString(R.string.select_language))
                    .create()
                    .show()
                dismiss()
            }
        }

        binding.wiki.setOnClickListener {
            dismiss()
            requireActivity().openLink("https://wotaku.wiki/guides/music/echo")
        }

        binding.version.run {
            val version = appVersion()
            text = version
            setOnClickListener {
                val info = buildString {
                    appendLine("Echo Version: $version")
                    appendLine("Device: $BRAND $DEVICE")
                    appendLine("Architecture: ${getArch()}")
                    appendLine("OS Version: $CODENAME $RELEASE ($SDK_INT)")
                }
                context.copyToClipboard(getString(R.string.version), info)
            }
            setOnLongClickListener {
                val viewModel by activityViewModel<ExtensionsViewModel>()
                viewModel.update(requireActivity(), true)
                dismiss()
                true
            }
        }

        configureBottomBar(binding.extensionBar)
        binding.extensionsCont.setOnClickListener {
            dismiss()
            ExtensionsListBottomSheet.newInstance(ExtensionType.MUSIC)
                .show(parentFragmentManager, null)
        }

        observe(viewModel.extensionLoader.current) { ext ->
            viewModel.currentExtension.value = ext
            binding.currentExtensionSettings.setOnClickListener {
                ext ?: return@setOnClickListener
                dismiss()
                requireActivity().openFragment<ExtensionInfoFragment>(
                    null, ExtensionInfoFragment.getBundle(ext)
                )
            }
            binding.extensions.loadBigIcon(ext?.metadata?.icon, R.drawable.ic_extension_32dp)
            binding.extensionsCont.setLoopedLongClick(
                viewModel.extensionLoader.music.value.filter { it.isEnabled },
                { viewModel.extensionLoader.current.value }
            ) {
                viewModel.extensionLoader.setupMusicExtension(it, true)
            }
        }

        binding.currentAccountName.setOnClickListener {
            dismiss()
            LoginUserListBottomSheet().show(parentFragmentManager, null)
        }
        binding.accountsCont.setOnClickListener {
            dismiss()
            LoginUserListBottomSheet().show(parentFragmentManager, null)
        }
        observe(viewModel.allUsersWithClient) { (ext, isLoginClient, all) ->
            binding.extensionBar.updateLayoutParams {
                width = if (isLoginClient) MATCH_PARENT else WRAP_CONTENT
            }
            binding.currentAccountName.run {
                isVisible = isLoginClient
                text = viewModel.currentUser.value?.name ?: getString(R.string.no_account)
                setLoopedLongClick(all, { all.find { it.second } }) {
                    ext ?: return@setLoopedLongClick
                    viewModel.setLoginUser(it.first.toEntity(ext.type, ext.id))
                }
            }
            binding.accounts.loadBigIcon(
                viewModel.currentUser.value?.cover, R.drawable.ic_account_circle_32dp
            )
            binding.accountsCont.run {
                isVisible = isLoginClient
                setLoopedLongClick(all, { all.find { it.second } }) {
                    ext ?: return@setLoopedLongClick
                    viewModel.setLoginUser(it.first.toEntity(ext.type, ext.id))
                }
            }
        }
    }
}