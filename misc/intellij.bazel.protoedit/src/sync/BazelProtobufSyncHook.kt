package org.jetbrains.bazel.protobuf

import com.intellij.openapi.components.serviceAsync
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.protobuf.target.ProtobufBuildTarget
import org.jetbrains.bazel.protobuf.target.extractProtobufBuildTarget
import org.jetbrains.bazel.sync.ProjectSyncHook

internal class BazelProtobufSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val store = environment.project.serviceAsync<BazelProtobufIndexService>().store
    val outputResolver = environment.server.outputResolver
    val localRepositories = environment.snapshot.repoMapping.getLocalRepositories()

    store.clearProtoIndexData()
    environment.snapshot
      .targets
      .allTargets()
      .mapNotNull { extractProtobufBuildTarget(it) }
      .forEach { protoData: ProtobufBuildTarget ->
        for ((importPath, location) in protoData.sources) {
          val path = outputResolver.resolve(location, localRepositories) ?: continue
          store.putProtoFullPath(importPath, path)
        }
      }
    store.save()
  }
}
