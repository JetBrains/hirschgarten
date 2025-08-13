package org.jetbrains.bazel.languages.starlark.bazel.modules

import com.intellij.openapi.components.service
import org.jetbrains.bazel.sync.ProjectPostSyncHook

class BazelModuleFetchSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    environment.project
      .service<BazelModuleRegistryService>()
      .refreshModuleNames()
  }
}
