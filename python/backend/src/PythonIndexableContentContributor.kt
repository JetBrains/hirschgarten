package com.intellij.bazel.python.backend

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.workspace.indexing.IndexableContent
import org.jetbrains.bazel.workspace.indexing.IndexableContentContributor
import java.nio.file.Path

internal class PythonIndexableContentContributor : IndexableContentContributor {

  override fun getIndexableContent(project: Project): IndexableContent {
    val rootDir = Path.of(project.rootDir.path)
    val sourcesIndex = project.service<PythonResolveIndexService>().resolveIndex
    val vFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    return IndexableContent(
      nonRecursiveRoots = sourcesIndex
        .values
        .filter { !it.startsWith(rootDir) }
        .flatMapTo(mutableSetOf()) { path ->
          // non-recursive root does not include children of a directory, so add the `__init__` file of a package explicitly
          val packageInitFiles = VirtualFileManager.getInstance().findFileByNioPath(path)
            ?.takeIf { it.isDirectory }
            ?.findPackageInitFiles()
            .orEmpty()
          (listOf(path) + packageInitFiles.map { it.toNioPath() })
            .map { it.toVirtualFileUrl(vFileUrlManager) }
        }
    )
  }
}
