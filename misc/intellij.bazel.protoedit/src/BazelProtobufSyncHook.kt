package org.jetbrains.bazel.protobuf

import com.intellij.openapi.components.serviceAsync
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bsp.protocol.ProtobufBuildTarget
import org.jetbrains.bsp.protocol.utils.extractProtobufBuildTarget
import java.nio.file.Path

internal class BazelProtobufSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val store = environment.project.serviceAsync<BazelProtobufIndexService>().store

    store.clearProtoIndexData()
    environment.workspace
      .targets
      .mapNotNull { extractProtobufBuildTarget(it) }
      .forEach { protoData: ProtobufBuildTarget ->
        for ((importPath, absolutePath) in protoData.sources) {
          store.putProtoFullPath(importPath, Path.of(absolutePath))
        }
      }
    store.save()
  }
}
