package org.jetbrains.bazel.ui.widgets.tool.window.all.targets

import com.intellij.openapi.client.ClientProjectSession
import com.intellij.ui.viewModel.extraction.ToolWindowExtractorMode
import com.intellij.ui.viewModel.extraction.ToolWindowViewModelExtractor
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject

internal class BazelToolWindowViewModelExtractor : ToolWindowViewModelExtractor {
  override fun isApplicable(toolWindowId: String, session: ClientProjectSession): Boolean =
    toolWindowId == BazelPluginConstants.BAZEL_TOOLWINDOW_ID && session.project.isBazelProject

  // force targets window to be shared as a LUX component,
  // for large number of targets sending every node update over RD is wasteful and
  // cause massive freezes(with monolithic updates)
  override fun getMode(): ToolWindowExtractorMode = ToolWindowExtractorMode.PROJECTOR_INSTANCING
}
