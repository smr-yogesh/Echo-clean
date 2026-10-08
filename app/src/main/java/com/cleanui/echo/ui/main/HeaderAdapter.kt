package com.cleanui.echo.ui.main

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.android.material.color.MaterialColors
import com.cleanui.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.User
import com.cleanui.echo.databinding.ItemMainHeaderBinding
import com.cleanui.echo.extensions.db.models.UserEntity.Companion.toEntity
import com.cleanui.echo.ui.common.GridAdapter
import com.cleanui.echo.ui.extensions.list.ExtensionsListBottomSheet
import com.cleanui.echo.ui.extensions.login.LoginUserListViewModel
import com.cleanui.echo.ui.settings.SettingsBottomSheet
import com.cleanui.echo.utils.ContextUtils.observe
import com.cleanui.echo.utils.image.ImageUtils.loadAsCircle
import com.cleanui.echo.utils.ui.scrolling.ScrollAnimRecyclerAdapter
import com.cleanui.echo.utils.ui.scrolling.ScrollAnimViewHolder
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class HeaderAdapter(
    private val fragment: Fragment,
    /** Optional title supplier (e.g. time-based greeting). Falls back to the extension name. */
    private val titleProvider: ((Context) -> String)? = null,
) : ScrollAnimRecyclerAdapter<HeaderAdapter.ViewHolder>(), GridAdapter {
    private val viewModel by fragment.activityViewModel<LoginUserListViewModel>()

    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = count
    override fun getItemCount() = 1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(parent)
        val binding = holder.binding
        val parentFragmentManager = fragment.parentFragmentManager
        binding.extensionsCont.setOnClickListener {
            ExtensionsListBottomSheet.newInstance(ExtensionType.MUSIC)
                .show(parentFragmentManager, null)
        }

        binding.accountsCont.setOnClickListener {
            SettingsBottomSheet().show(parentFragmentManager, null)
        }
        return holder
    }

    private var extension: MusicExtension? = null
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    private var triple: Triple<Extension<*>?, Boolean, List<Pair<User, Boolean>>> =
        Triple(null, false, emptyList())
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        super.onBindViewHolder(holder, position)
        val context = root.context
        title.text = titleProvider?.invoke(context)
            ?: extension?.name ?: context.getString(R.string.app_name)
        val iconTint = context.getColor(R.color.icon_on_light)
        extensions.loadBigIcon(
            extension?.metadata?.icon, R.drawable.ic_extension_32dp, iconTint
        )
        extensionsCont.setLoopedLongClick(
            viewModel.extensionLoader.music.value.filter { it.isEnabled },
            { viewModel.extensionLoader.current.value }
        ) {
            viewModel.extensionLoader.setupMusicExtension(it, true)
        }

        val (ext, isLoginClient, all) = triple
        accounts.loadBigIcon(
            viewModel.currentUser.value?.cover,
            if (isLoginClient) R.drawable.ic_account_circle_32dp else R.drawable.ic_settings_outline_32dp,
            iconTint
        )
        accountsCont.setLoopedLongClick(all, { all.find { it.second } }) {
            ext ?: return@setLoopedLongClick
            viewModel.setLoginUser(it.first.toEntity(ext.type, ext.id))
        }
    }

    class ViewHolder(
        parent: ViewGroup,
        val binding: ItemMainHeaderBinding = ItemMainHeaderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : ScrollAnimViewHolder(binding.root)

    init {
        with(fragment) {

            observe(viewModel.extensionLoader.current) {
                viewModel.currentExtension.value = it
                extension = it
            }
            observe(viewModel.allUsersWithClient) {
                triple = it
            }
        }
    }

    companion object {
        private var greetingPhrases: List<String>? = null

        private fun greetingPhrases(context: Context): List<String> = greetingPhrases ?: run {
            val ids = intArrayOf(R.string.good_morning, R.string.good_afternoon, R.string.good_evening)
            val phrases = mutableListOf("what's on your mind")
            for (tag in listOf("en", "de", "fr", "es", "it", "pt")) {
                val config = android.content.res.Configuration(context.resources.configuration)
                config.setLocale(java.util.Locale.forLanguageTag(tag))
                val localized = context.createConfigurationContext(config)
                ids.forEach { phrases += localized.getString(it).trim().lowercase() }
            }
            phrases.distinct().also { greetingPhrases = it }
        }

        /**
         * True when [text] looks like an extension-provided greeting ("Good evening", "Guten Abend",
         * "What's on your mind?"). Used on Home so only one greeting is shown, whichever extension
         * is active.
         */
        fun isGreeting(context: Context, text: String?): Boolean {
            val value = text?.trim()?.lowercase().orEmpty()
            if (value.isEmpty()) return false
            return greetingPhrases(context).any { value.startsWith(it) }
        }

        /** Time-of-day greeting for the Home header (05-12 morning, 12-18 afternoon, else evening). */
        fun greeting(context: Context): String {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            return context.getString(
                when (hour) {
                    in 5..11 -> R.string.good_morning
                    in 12..17 -> R.string.good_afternoon
                    else -> R.string.good_evening
                }
            )
        }

        fun <T> View.setLoopedLongClick(
            list: List<T>,
            getCurrent: (View) -> T?,
            onSelect: (T) -> Unit,
        ) {
            setOnLongClickListener {
                val current = getCurrent(this)
                val index = list.indexOf(current)
                if (index == -1) return@setOnLongClickListener false
                val next = list[(index + 1) % list.size]
                if (next == current) return@setOnLongClickListener false
                onSelect(next)
                true
            }
        }

        fun ImageView.loadBigIcon(image: ImageHolder?, placeholder: Int, tint: Int? = null) {
            val color = ColorStateList.valueOf(
                tint ?: MaterialColors.getColor(
                    this,
                    androidx.appcompat.R.attr.colorControlNormal
                )
            )
            image.loadAsCircle(this) {
                if (it == null) {
                    imageTintList = color
                    setImageResource(placeholder)
                } else {
                    imageTintList = null
                    setImageDrawable(it)
                }
            }
        }
    }
}