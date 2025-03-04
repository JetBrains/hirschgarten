package org.jetbrains.bazel.intellij

import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

private const val INTELLIJ_PLUGIN_TAG = "intellij-plugin"

class IntellijPluginRunHandlerProvider : RunHandlerProvider {
  override val id: String = "IntellijPluginRunHandlerProvider"

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = IntellijPluginRunHandler(configuration)

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
    targetInfos.singleOrNull()?.tags?.contains(INTELLIJ_PLUGIN_TAG)
      ?: false

  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = canRun(targetInfos)
}
