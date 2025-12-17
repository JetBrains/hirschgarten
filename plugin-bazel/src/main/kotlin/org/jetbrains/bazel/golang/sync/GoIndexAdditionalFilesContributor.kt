package org.jetbrains.bazel.golang.sync

import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspace.indexAdditionalFiles.IndexAdditionalFilesContributor
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget

class GoIndexAdditionalFilesContributor : IndexAdditionalFilesContributor {
  override fun getAdditionalFiles(project: Project): List<VirtualFileUrl> {
    val vFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    return project.targetUtils
      .allBuildTargets()
      .mapNotNull { extractGoBuildTarget(it) }
      .flatMap { it.generatedSources }
      .map { it.toVirtualFileUrl(vFileUrlManager) }
      .toList()
  }
}
