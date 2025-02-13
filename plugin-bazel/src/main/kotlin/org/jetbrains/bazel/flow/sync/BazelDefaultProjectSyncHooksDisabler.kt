package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.sync.DefaultProjectSyncHooksDisabler
import org.jetbrains.plugins.bsp.sync.ProjectSyncHook
import org.jetbrains.plugins.bsp.sync.defaults.OutputPathUrisSyncHook

class BazelDefaultProjectSyncHooksDisabler : DefaultProjectSyncHooksDisabler {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun disabledProjectSyncHooks(project: Project): List<Class<out ProjectSyncHook>> = listOf(OutputPathUrisSyncHook::class.java)
}
