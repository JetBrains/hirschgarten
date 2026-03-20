package org.jetbrains.bazel.ui.widgets.tool.window.all.targets

import com.intellij.openapi.client.ClientProjectSession
import com.intellij.ui.viewModel.extraction.ToolWindowExtractorMode
import com.intellij.ui.viewModel.extraction.ToolWindowViewModelExtractor
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.bazelProjectProperties

private class BazelToolWindowViewModelExtractor : ToolWindowViewModelExtractor {
  override fun isApplicable(toolWindowId: String, session: ClientProjectSession): Boolean =
    toolWindowId == BazelPluginConstants.BAZEL_TOOLWINDOW_ID && session.project.bazelProjectProperties.isBazelProject

  override fun getMode(): ToolWindowExtractorMode = ToolWindowExtractorMode.MIRROR
}
