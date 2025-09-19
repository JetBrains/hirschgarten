package org.jetbrains.bazel.languages.starlark.bazel.bzlmod

import org.jetbrains.bazel.sync.ProjectPostSyncHook

class BazelModuleFetchSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    BazelModuleRegistryService
      .getInstance(environment.project)
      .refreshModuleNames()
  }
}
