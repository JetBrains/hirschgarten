package org.jetbrains.bazel.protobuf

import com.intellij.openapi.components.serviceAsync
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.ProtobufBuildTarget
import java.nio.file.Path

class BazelProtobufSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val store = environment.project.serviceAsync<BazelProtobufSyncService>().store

    store.clearProtoIndexData()
    environment.project.targetUtils
      .allBuildTargets()
      .filter { it.kind.languageClasses.contains(LanguageClass.PROTOBUF) }
      .forEach {
        val protoData = it.data as? ProtobufBuildTarget ?: return@forEach
        for ((importPath, absolutePath) in protoData.sources) {
          store.putProtoIndexData(BazelProtobufSyncIndexData(importPath = importPath, absolutePath = Path.of(absolutePath)))
        }
      }
  }
}
