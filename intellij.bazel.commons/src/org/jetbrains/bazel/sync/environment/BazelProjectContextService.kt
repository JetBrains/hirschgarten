package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelRelease

@ApiStatus.Internal
interface BazelProjectContextService {
  var isBazelProject: Boolean
  var projectRootDir: VirtualFile?
  var workspaceName: String?

  // TODO: use NIO paths for bazel `bin` and `exec` paths
  var bazelBinPath: String?
  var bazelExecPath: String?

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
