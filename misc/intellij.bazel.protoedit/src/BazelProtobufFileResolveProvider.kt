package org.jetbrains.bazel.protobuf

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.protobuf.ide.settings.SettingsFileResolveProvider
import com.intellij.protobuf.lang.resolve.FileResolveProvider
import com.intellij.psi.search.GlobalSearchScope

internal class  BazelProtobufFileResolveProvider : FileResolveProvider {
  override fun findFile(path: String, project: Project): VirtualFile? {
    if (!path.endsWith(".proto")) {
      return null
    }
    // Well-known protos (including google/protobuf/descriptor.proto, which standard options such as
    // `java_package` resolve against) are owned by the Protobuf plugin's bundled copies. Resolving
    // them here too would make imports like google/protobuf/timestamp.proto point at two distinct
    // files and be flagged as "Ambiguous import".
    if (SettingsFileResolveProvider().findFile(path, project) != null)
      return null

    return project
      .service<BazelProtobufIndexService>()
      .getRealProtoFile(path)
  }

  override fun getChildEntries(path: String, project: Project): Collection<FileResolveProvider.ChildEntry?> = emptyList()

  override fun getDescriptorFile(project: Project): VirtualFile? = null

  override fun getSearchScope(project: Project): GlobalSearchScope = GlobalSearchScope.projectScope(project)
}
