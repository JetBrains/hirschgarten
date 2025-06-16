package org.jetbrains.bazel.ui.settings

import com.intellij.ide.ui.search.SearchableOptionContributor
import com.intellij.ide.ui.search.SearchableOptionProcessor
import org.jetbrains.bazel.config.BazelPluginBundle

class BazelSearchOptionsContributor : SearchableOptionContributor() {
  override fun processOptions(processor: SearchableOptionProcessor) {
    val settingPages = listOf(bazelProjectSettingsPage, bazelApplicationSettingsPage, bazelExperimentalProjectSettingsPage)
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
    optionBundleKeys = BazelProjectSettingsConfigurable.SearchIndex.keys + BazelGeneralSettingsProvider.searchIndexKeys(),
  )

private val bazelExperimentalProjectSettingsPage =
  SettingsPage(
    id = BazelExperimentalProjectSettingsConfigurable.ID,
    displayNameKey = BazelExperimentalProjectSettingsConfigurable.DISPLAY_NAME_KEY,
    optionBundleKeys = BazelExperimentalSettingsProvider.searchIndexKeys(),
  )

private val bazelApplicationSettingsPage =
  SettingsPage(
    id = BazelApplicationSettingsConfigurable.ID,
    displayNameKey = BazelApplicationSettingsConfigurable.DISPLAY_NAME_KEY,
    optionBundleKeys = BazelApplicationSettingsConfigurable.SearchIndex.keys,
  )
