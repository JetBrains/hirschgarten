package org.jetbrains.bazel.ui.settings

import com.intellij.ide.ui.search.SearchableOptionContributor
import com.intellij.ide.ui.search.SearchableOptionProcessor
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.formatting.configuration.BuildifierConfigurable
import org.jetbrains.bazel.ui.settings.BazelApplicationSettingsConfigurable

class BazelSearchOptionsContributor : SearchableOptionContributor() {
  override fun processOptions(processor: SearchableOptionProcessor) {
    val settingPages = listOf(bazelProjectSettingsPage, bazelApplicationSettingsPage, buildifierSettingsPage)
    settingPages.forEach { page ->
      val displayName = page.cleanDisplayName()
      page.optionBundleKeys.forEach {
        val hit = BazelPluginBundle.message(it)
        processor.addOptions(hit, null, hit, page.id, displayName, false)
      }
    }
  }
}

private data class SettingsPage(
  val id: String,
  val displayNameKey: String,
  val optionBundleKeys: List<String>,
) {
  fun cleanDisplayName(): String = BazelPluginBundle.message(displayNameKey).dropLastWhile { it.isWhitespace() || it == ':' }
}

private val bazelProjectSettingsPage =
  SettingsPage(
    id = BazelProjectSettingsConfigurable.ID,
    displayNameKey = BazelProjectSettingsConfigurable.DISPLAY_NAME_KEY,
    optionBundleKeys = BazelProjectSettingsConfigurable.SearchIndex.keys,
  )

private val bazelApplicationSettingsPage =
  SettingsPage(
    id = BazelApplicationSettingsConfigurable.ID,
    displayNameKey = BazelApplicationSettingsConfigurable.DISPLAY_NAME_KEY,
    optionBundleKeys = BazelApplicationSettingsConfigurable.SearchIndex.keys,
  )

private val buildifierSettingsPage =
  SettingsPage(
    id = BuildifierConfigurable.ID,
    displayNameKey = BuildifierConfigurable.DISPLAY_NAME_KEY,
    optionBundleKeys = BuildifierConfigurable.SearchIndex.keys,
  )
