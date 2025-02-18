package org.jetbrains.bazel.impl.flow.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.sync.DefaultProjectSyncHooksDisabler
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment

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
