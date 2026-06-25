package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelRelease
import java.nio.file.Path

@ApiStatus.Internal
interface BazelProjectContextService {
  val isBazelProject: Boolean
  val projectRootDir: VirtualFile?

  var workspaceName: String?
  var bazelBinPath: Path?
  var bazelExecPath: Path?

  var bazelRelease: BazelRelease?

  val avoidExternalSystem: Boolean
    get() = false
}

val Project.projectCtx: BazelProjectContextService
  @ApiStatus.Internal
  get() = service<BazelProjectContextService>()

@ApiStatus.Internal
fun BazelProjectContextService.getProjectRootDirOrThrow(): VirtualFile {
  return projectRootDir ?: error("Project root directory is not initialized")
}
