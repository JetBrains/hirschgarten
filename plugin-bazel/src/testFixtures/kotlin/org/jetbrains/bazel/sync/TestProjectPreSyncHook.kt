package org.jetbrains.bazel.sync

import com.intellij.openapi.project.Project

open class TestProjectPreSyncHook : ProjectPreSyncHook {
  var wasCalled: Boolean = false

  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    wasCalled = true
  }
}

class DisabledTestProjectPreSyncHook : TestProjectPreSyncHook() {
  override fun isEnabled(project: Project): Boolean = false
}
