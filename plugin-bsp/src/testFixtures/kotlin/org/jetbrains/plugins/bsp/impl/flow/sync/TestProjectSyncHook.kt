package org.jetbrains.plugins.bsp.impl.flow.sync

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook.ProjectSyncHookEnvironment

open class TestProjectSyncHook(override val buildToolId: BuildToolId) : ProjectSyncHook {
  var wasCalled: Boolean = false

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    wasCalled = true
  }
}

class DisabledTestProjectSyncHook(override val buildToolId: BuildToolId) : TestProjectSyncHook(buildToolId) {
  override fun isEnabled(project: Project): Boolean = false
}

class TestDefaultProjectSyncDisabler(override val buildToolId: BuildToolId, private val toDisable: List<Class<out ProjectSyncHook>>) :
  DefaultProjectSyncHooksDisabler {
  override fun disabledProjectSyncHooks(project: Project): List<Class<out ProjectSyncHook>> = toDisable
}
