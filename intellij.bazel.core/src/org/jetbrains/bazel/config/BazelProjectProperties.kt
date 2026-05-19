package org.jetbrains.bazel.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.sync.environment.getProjectRootDirOrThrow
import org.jetbrains.bazel.sync.environment.projectCtx
import kotlin.DeprecationLevel.ERROR

// TODO Remove when Scala plugin no longer uses these methods
@Deprecated("Exists only for binary compatibility", level = ERROR)
fun isBazelProject(project: Project): Boolean = project.projectCtx.isBazelProject
@Deprecated("Exists only for binary compatibility", level = ERROR)
fun getRootDir(project: Project): VirtualFile = project.projectCtx.getProjectRootDirOrThrow()
