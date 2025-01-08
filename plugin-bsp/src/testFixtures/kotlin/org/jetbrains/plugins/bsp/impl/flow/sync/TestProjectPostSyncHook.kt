package org.jetbrains.plugins.bsp.impl.flow.sync

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BuildToolId

open class TestProjectPostSyncHook(override val buildToolId: BuildToolId) : ProjectPostSyncHook {
  var wasCalled: Boolean = false

  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    wasCalled = true
  }
}

class DisabledTestProjectPostSyncHook(override val buildToolId: BuildToolId) : TestProjectPostSyncHook(buildToolId) {
  override fun isEnabled(project: Project): Boolean = false
}
