package org.jetbrains.bazel.ui.settings

import com.intellij.ui.ColoredListCellRenderer
import javax.swing.JList

private const val BAZEL_PLUGIN_MARKETPLACE_ID = "22977"

private const val BASE_PLUGIN_MARKETPLACE_URL = "https://plugins.jetbrains.com/plugins/%s/"
private const val BAZEL_PLUGIN_MARKETPLACE_URL = BASE_PLUGIN_MARKETPLACE_URL + BAZEL_PLUGIN_MARKETPLACE_ID

private const val NIGHTLY_MARKETPLACE_CHANNEL = "nightly"

enum class UpdateChannel(val displayName: String, val bazelPluginUrl: String) {
  /**
   * If we don't override plugin repositories with custom URLs,
   * the IDEA will automatically use the default channel for retrieving plugin URLs.
   * The entry [RELEASE] is here for settings and completeness and does not need the marketplace URLs.
   */
  RELEASE("Release", ""),
  NIGHTLY(
    "Nightly",
    BAZEL_PLUGIN_MARKETPLACE_URL.format(NIGHTLY_MARKETPLACE_CHANNEL),
  ),
  ;

  fun getPluginUrlFromId(id: String): String =
    when (id) {
      BAZEL_PLUGIN_ID -> bazelPluginUrl
      else -> ""
    }

  class Renderer : ColoredListCellRenderer<UpdateChannel>() {
    override fun customizeCellRenderer(
      list: JList<out UpdateChannel?>,
      value: UpdateChannel?,
      index: Int,
      selected: Boolean,
      hasFocus: Boolean,
    ) {
      append(value?.displayName ?: "")
    }
  }
}
