package org.jetbrains.bazel.workspace.indexAdditionalFiles

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

interface IndexAdditionalFilesContributor {
  fun getAdditionalFiles(project: Project): List<VirtualFileUrl>

  companion object {
    val ep = ExtensionPointName<IndexAdditionalFilesContributor>("org.jetbrains.bazel.indexAdditionalFilesContributor")
  }
}
