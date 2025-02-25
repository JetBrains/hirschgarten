package org.jetbrains.bazel.impl.flow.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.ProjectPostSyncHook

open class TestProjectPostSyncHook : ProjectPostSyncHook {
  var wasCalled: Boolean = false

  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    wasCalled = true
  }
}

class DisabledTestProjectPostSyncHook : TestProjectPostSyncHook() {
  override fun isEnabled(project: Project): Boolean = false
}
