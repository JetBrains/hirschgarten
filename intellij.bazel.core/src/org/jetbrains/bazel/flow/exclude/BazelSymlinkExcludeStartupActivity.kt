package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.bazelProjectProperties
import java.nio.file.Path

/**
 * Scans the project workspace for Bazel convenience symlinks and excludes them when the project opens.
 * It intentionally doesn't check if it's a Bazel project, so it will work even when
 * the user hasn't yet requested to attach the Bazel support to the project.
 */
@ApiStatus.Internal
class BazelSymlinkExcludeStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    val bazelWorkspace = project.guessBazelWorkspaceDir() ?: return
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(project)
    bazelSymlinkExcludeService.scanForBazelSymlinksToExclude(bazelWorkspace)
    edtWriteAction {
      bazelSymlinkExcludeService.refreshWorkspaceModel()
    }
  }

  private fun Project.guessBazelWorkspaceDir(): Path? {
    val virtualFile = bazelProjectProperties.rootDir ?: guessProjectDir()
    return virtualFile?.toNioPathOrNull()
  }
}
