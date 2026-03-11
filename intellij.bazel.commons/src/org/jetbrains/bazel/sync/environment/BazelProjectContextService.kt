package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface BazelProjectContextService {
  var isBazelProject: Boolean
  var projectRootDir: VirtualFile?
  var workspaceName: String?

  // TODO: use NIO paths for bazel `bin` and `exec` paths
  var bazelBinPath: String?
  var bazelExecPath: String?

  val avoidExternalSystem: Boolean
    get() = false

  val targetPersistenceLayer: BazelTargetPersistenceLayer
}

val Project.projectCtx: BazelProjectContextService
  @ApiStatus.Internal
  get() = service<BazelProjectContextService>()

@ApiStatus.Internal
fun BazelProjectContextService.getProjectRootDirOrThrow(): VirtualFile {
  return projectRootDir ?: error("Project root directory is not initialized")
}
