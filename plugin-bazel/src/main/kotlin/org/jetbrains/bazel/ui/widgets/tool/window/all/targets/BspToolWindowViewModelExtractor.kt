package org.jetbrains.bazel.ui.widgets.tool.window.all.targets

import com.intellij.openapi.client.ClientProjectSession
import com.intellij.ui.viewModel.extraction.ToolWindowExtractorMode
import com.intellij.ui.viewModel.extraction.ToolWindowViewModelExtractor

class BspToolWindowViewModelExtractor : ToolWindowViewModelExtractor {
  override fun isApplicable(toolWindowId: String, session: ClientProjectSession): Boolean =
    session.project.bspToolWindowIdOrNull == toolWindowId

  override fun getMode(): ToolWindowExtractorMode = ToolWindowExtractorMode.MIRROR
}
