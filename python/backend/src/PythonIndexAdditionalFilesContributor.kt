package com.intellij.bazel.python.backend

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.workspace.indexAdditionalFiles.IndexAdditionalFilesContributor
import java.nio.file.Path

internal class PythonIndexAdditionalFilesContributor : IndexAdditionalFilesContributor {
  override fun getAdditionalFiles(project: Project): List<VirtualFileUrl> {
    val rootDir = Path.of(project.rootDir.path)
    val sourcesIndex = project.service<PythonResolveIndexService>().resolveIndex
    val vFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    return sourcesIndex.values.filter { !it.startsWith(rootDir) }
      .map { path -> path.toVirtualFileUrl(vFileUrlManager) }
  }
}
