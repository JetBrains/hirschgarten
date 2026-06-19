package org.jetbrains.bazel.protobuf

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import java.io.IOException

@Service(Service.Level.PROJECT)
internal class BazelProtobufIndexService(val project: Project) {
  val store = BazelProtobufIndexStore(project)

  fun getRealProtoFile(importPath: String): VirtualFile? {
    val fullPath = store.getProtoFullPath(importPath) ?: return null
    val vfsManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    // handle case when bazel hadn't created protobuf symlinks yet
    // or user deleted file referenced by symlink
    val realPath =
      try {
        fullPath.toRealPath()
      } catch (_: IOException) {
        fullPath
      }
    return realPath
      .toVirtualFileUrl(vfsManager)
      .virtualFile
  }
}
