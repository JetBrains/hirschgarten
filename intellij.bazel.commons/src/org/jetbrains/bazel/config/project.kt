package org.jetbrains.bazel.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.environment.getProjectRootDirOrThrow
import org.jetbrains.bazel.sync.environment.projectCtx

var Project.isBazelProject: Boolean
  get() = projectCtx.isBazelProject
  @ApiStatus.Internal
  set(value) {
    projectCtx.isBazelProject = value
  }

var Project.rootDir: VirtualFile
  get() = projectCtx.getProjectRootDirOrThrow()
  @ApiStatus.Internal
  set(value) {
    projectCtx.projectRootDir = value
  }

var Project.workspaceName: String?
  @ApiStatus.Internal
  get() = projectCtx.workspaceName
  @ApiStatus.Internal
  set(value) {
    projectCtx.workspaceName = value
  }

val Project.bazelProjectName: String
  @ApiStatus.Internal
  get() = rootDir.name
