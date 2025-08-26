package org.jetbrains.bazel.protobuf

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.protobuf.lang.resolve.FileResolveProvider
import com.intellij.psi.search.GlobalSearchScope

class BazelProtobufFileResolveProvider : FileResolveProvider {
  override fun findFile(path: String, project: Project): VirtualFile? {
    if (!path.endsWith(".proto")) {
      return null
    }
    return project.service<BazelProtobufSyncService>()
      .getRealProtoFile(path)
  }

  override fun getChildEntries(
    path: String,
    project: Project,
  ): Collection<FileResolveProvider.ChildEntry?> = emptyList()

  override fun getDescriptorFile(project: Project): VirtualFile? = null

  override fun getSearchScope(project: Project): GlobalSearchScope {
    return GlobalSearchScope.projectScope(project)
  }
}
