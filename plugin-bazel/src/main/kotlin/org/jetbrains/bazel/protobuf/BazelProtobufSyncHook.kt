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

    val protoPathToSources = mutableMapOf<String, BazelProtobufSyncIndexData>()
    environment.project.targetUtils.allBuildTargets()
      .filter { it.kind.languageClasses.contains(LanguageClass.PROTOBUF) }
      .forEach {
        val protoData = it.data as? ProtobufBuildTarget ?: return@forEach
        for ((importPath, realPath) in protoData.sources) {
          protoPathToSources.computeIfAbsent(importPath) { _ -> BazelProtobufSyncIndexData(importPath, it.baseDirectory) }
            .realPaths.add(Path.of(realPath))
        }
      }

    protoPathToSources.forEach { store.putProtoIndexData(it.value) }
  }
}
