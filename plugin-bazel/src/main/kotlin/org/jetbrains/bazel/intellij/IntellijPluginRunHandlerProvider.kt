package org.jetbrains.bazel.intellij

import org.jetbrains.bazel.run.BspRunHandler
import org.jetbrains.bazel.run.BspRunHandlerProvider
import org.jetbrains.bazel.run.config.BspRunConfiguration
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

private const val INTELLIJ_PLUGIN_TAG = "intellij-plugin"

class IntellijPluginRunHandlerProvider : BspRunHandlerProvider {
  override val id: String = "IntellijPluginRunHandlerProvider"

  override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = IntellijPluginRunHandler(configuration)

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
    targetInfos.singleOrNull()?.tags?.contains(INTELLIJ_PLUGIN_TAG)
      ?: false

  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = canRun(targetInfos)
}
