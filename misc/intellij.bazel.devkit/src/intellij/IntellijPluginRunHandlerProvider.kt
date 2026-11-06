package org.jetbrains.bazel.intellij

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bsp.protocol.BuildTarget

internal class IntellijPluginRunHandlerProvider : RunHandlerProvider {
  override val id: String
    get() = "IntellijPluginRunHandlerProvider"

  override val providedRunHandlerName: @Nls String
    get() = BazelDevKitBundle.message("intellij.plugin.run.handler.name")

  override val isTestHandler: Boolean get() = false

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = IntellijPluginRunHandler(configuration)

  override fun canRun(project: Project, targets: List<BuildTarget>): Boolean =
    targets.singleOrNull()?.kind?.kind == "intellij_plugin_debug_target"
}
