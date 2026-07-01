package org.jetbrains.bazel.impl.flow.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.ProjectPostSyncHook

open class TestProjectPostSyncHook : ProjectPostSyncHook {
  var wasCalled: Boolean = false
  var projectModelUpdated: Boolean? = null

  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    wasCalled = true
    projectModelUpdated = environment.projectModelUpdated
  }
}

class DisabledTestProjectPostSyncHook : TestProjectPostSyncHook() {
  override fun isEnabled(project: Project): Boolean = false
}
