package org.jetbrains.bazel.python.sync

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.python.resolve.PythonResolveIndexService
import org.jetbrains.bazel.workspace.indexAdditionalFiles.IndexAdditionalFilesContributor
import java.nio.file.Path
import kotlin.io.path.pathString

internal class PythonIndexAdditionalFilesContributor : IndexAdditionalFilesContributor {
  override fun getAdditionalFiles(project: Project): List<VirtualFileUrl> {
    val rootDir = Path.of(project.rootDir.path)
    val sourcesIndex = project.service<PythonResolveIndexService>().resolveIndex
    val vFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    return sourcesIndex.values.filter {
      !it.startsWith(rootDir)
    }.map { path ->
      vFileUrlManager.fromPath(path.pathString)
    }
  }
}
