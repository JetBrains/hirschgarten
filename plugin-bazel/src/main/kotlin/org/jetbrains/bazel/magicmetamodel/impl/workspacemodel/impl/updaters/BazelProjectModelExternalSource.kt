package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.roots.ProjectModelExternalSource
import org.jetbrains.bazel.config.BazelPluginConstants

object BazelProjectModelExternalSource : ProjectModelExternalSource {
  override fun getDisplayName(): String = BazelPluginConstants.BAZEL_DISPLAY_NAME

  override fun getId(): String = BazelPluginConstants.ID
}
