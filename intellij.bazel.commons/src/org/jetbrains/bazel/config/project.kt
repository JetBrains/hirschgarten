package org.jetbrains.bazel.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.environment.getProjectRootDirOrThrow
import org.jetbrains.bazel.sync.environment.projectCtx

val Project.isBazelProject: Boolean
  get() = projectCtx.isBazelProject

val Project.rootDir: VirtualFile
  get() = projectCtx.getProjectRootDirOrThrow()

val Project.bazelProjectName: String
  @ApiStatus.Internal
  get() = rootDir.name
