package org.jetbrains.bazel.protobuf

import com.intellij.openapi.components.service
import org.jetbrains.bazel.sync.ProjectPostSyncHook

class BazelProtobufSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    environment.project.service<BazelProtobufSyncService>().reindex()
  }
}
