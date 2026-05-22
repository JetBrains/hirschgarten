package org.jetbrains.bazel.ui.widgets.queryTab

import com.intellij.openapi.client.ClientProjectSession
import com.intellij.ui.viewModel.extraction.ToolWindowExtractorMode
import com.intellij.ui.viewModel.extraction.ToolWindowViewModelExtractor
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject

internal class BazelQueryToolWindowViewModelExtractor : ToolWindowViewModelExtractor {
  override fun isApplicable(toolWindowId: String, session: ClientProjectSession): Boolean =
    toolWindowId == BazelPluginConstants.BAZEL_QUERY_TOOLWINDOW_ID && session.project.isBazelProject

  override fun getMode(): ToolWindowExtractorMode = ToolWindowExtractorMode.MIRROR
}
