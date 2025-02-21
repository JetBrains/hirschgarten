package org.jetbrains.bazel.impl.flow.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.sync.ProjectPreSyncHook

open class TestProjectPreSyncHook(override val buildToolId: BuildToolId) : ProjectPreSyncHook {
  var wasCalled: Boolean = false

  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    wasCalled = true
  }
}

class DisabledTestProjectPreSyncHook(override val buildToolId: BuildToolId) : TestProjectPreSyncHook(buildToolId) {
  override fun isEnabled(project: Project): Boolean = false
}
