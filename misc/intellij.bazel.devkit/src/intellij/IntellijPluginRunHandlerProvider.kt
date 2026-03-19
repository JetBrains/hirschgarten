package org.jetbrains.bazel.intellij

import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration

internal class IntellijPluginRunHandlerProvider : RunHandlerProvider {
  override val id: String
    get() = "IntellijPluginRunHandlerProvider"

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = IntellijPluginRunHandler(configuration)

  override fun canRun(targets: List<TargetKind>): Boolean =
    targets.singleOrNull()?.kind == "intellij_plugin_debug_target"

  override fun canDebug(targets: List<TargetKind>): Boolean = canRun(targets)
}
