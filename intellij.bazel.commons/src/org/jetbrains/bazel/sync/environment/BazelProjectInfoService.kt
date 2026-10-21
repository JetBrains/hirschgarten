package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface BazelProjectInfoService {
  val isBazelProject: Boolean
  val projectRootDir: VirtualFile?
}

val Project.bazelProjectInfo: BazelProjectInfoService
  @ApiStatus.Internal
  get() = service<BazelProjectInfoService>()

@ApiStatus.Internal
fun BazelProjectInfoService.getProjectRootDirOrThrow(): VirtualFile {
  return projectRootDir ?: error("Project root directory is not initialized")
}
