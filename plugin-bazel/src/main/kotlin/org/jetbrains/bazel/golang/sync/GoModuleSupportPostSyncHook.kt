package org.jetbrains.bazel.golang.sync

import com.goide.project.GoModuleSettings
import com.intellij.openapi.application.writeAction
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.projectStructure.legacy.MonoModuleUtils

class GoModuleSupportPostSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val project = environment.project
    val monoModule = MonoModuleUtils.findModule(project) ?: return
    writeAction {
      GoModuleSettings.getInstance(monoModule).isGoSupportEnabled = true
    }
  }
}
