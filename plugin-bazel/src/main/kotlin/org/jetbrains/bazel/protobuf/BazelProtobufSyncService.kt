package org.jetbrains.bazel.protobuf

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.ProtobufBuildTarget
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Path
import kotlin.io.path.absolutePathString

@Service(Service.Level.PROJECT)
@State(name = "ProtobufResolveIndexService", storages = [Storage("bazelProtobuf.xml")], reportStatistic = true)
class BazelProtobufSyncService(val project: Project) : LazyPersistentCachedStateComponent<BazelProtobufSyncService.State>() {

  data class State(val importToSourceIndex: Map<String, BazelProtobufSyncIndexData> = emptyMap())

  fun getRealProtoFile(importPath: String): VirtualFile? {
    val data = this.myState.importToSourceIndex[importPath] ?: return null
    val vfsManager = project.service<WorkspaceModel>().getVirtualFileUrlManager()
    for (realSource in data.realPaths) {
      val vfsFile = realSource.toVirtualFileUrl(vfsManager).virtualFile
      if (vfsFile != null && vfsFile.exists()) {
        return vfsFile
      }
    }
    return null
  }

  override fun createIndex(): BazelProtobufSyncService.State {
    val newImportToSourceIndex = mutableMapOf<String, BazelProtobufSyncIndexData>()
    project.targetUtils.allBuildTargets()
      .filter { it.kind.languageClasses.contains(LanguageClass.PROTOBUF) }
      .forEach {
        val protoData = it.data as? ProtobufBuildTarget ?: return@forEach
        for ((importPath, realPath) in protoData.sources) {
          newImportToSourceIndex.computeIfAbsent(importPath) { _ -> BazelProtobufSyncIndexData(it.baseDirectory) }
            .realPaths.add(Path.of(realPath))
        }
      }
    return State(newImportToSourceIndex)
  }

  override fun encodeState(stream: DataOutputStream, state: BazelProtobufSyncService.State) {
    val map = state.importToSourceIndex
    stream.writeInt(map.size)
    for ((k, v) in map) {
      stream.writeUTF(k)
      stream.writeUTF(v.root.absolutePathString())
      stream.writeInt(v.realPaths.size)
      for (realPath in v.realPaths) {
        stream.writeUTF(realPath.absolutePathString())
      }
    }
  }

  override fun decodeState(stream: DataInputStream): BazelProtobufSyncService.State {
    val mapSize = stream.readInt()
    val map = mutableMapOf<String, BazelProtobufSyncIndexData>()
    for (n in 0 until mapSize) {
      val key = stream.readUTF()
      val root = stream.readUTF()
      val realPathsSize = stream.readInt()
      val realPaths = mutableListOf<Path>()
      for (i in 0 until realPathsSize) {
        realPaths.add(Path.of(stream.readUTF()))
      }
      map[key] = BazelProtobufSyncIndexData(Path.of(root), realPaths)
    }
    return State(map)
  }


}
