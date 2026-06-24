package org.jetbrains.bazel.flow.open

import com.intellij.ide.trustedProjects.TrustedProjectsLocator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import java.nio.file.Path

internal class BazelTrustedProjectsLocator : TrustedProjectsLocator {
  override fun getProjectRoots(project: Project): List<Path> {
    if (!project.isBazelProject) return emptyList()
    return listOfNotNull(project.rootDir.toNioPathOrNull())
  }

  override fun getProjectRoots(projectRoot: Path, project: Project?): List<Path> {
    // 'projectRoot' despite its name, may be a file when bazel plugin opens MODULE.bazel, *.bazelproject, etc.
    return listOfNotNull(findProjectFolderFromFile(projectRoot))
  }
}
