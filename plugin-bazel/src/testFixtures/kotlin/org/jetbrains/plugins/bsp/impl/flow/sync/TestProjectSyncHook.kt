package org.jetbrains.bazel.impl.flow.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment

open class TestProjectSyncHook : ProjectSyncHook {
  var wasCalled: Boolean = false

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    wasCalled = true
  }
}

class DisabledTestProjectSyncHook : TestProjectSyncHook() {
  override fun isEnabled(project: Project): Boolean = false
}
