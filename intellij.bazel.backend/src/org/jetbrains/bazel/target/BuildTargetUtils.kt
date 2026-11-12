package org.jetbrains.bazel.target

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetInfo
import org.jetbrains.bsp.protocol.OutputLocation
import java.nio.file.Path
import kotlin.collections.contains

@get:ApiStatus.Internal
val BuildTargetInfo.baseDirectoryLocation: OutputLocation
  get() {
    val label = id
    val packagePath = label.packagePath.toString()
    return if (label is ResolvedLabel && !label.isMainWorkspace) {
      OutputLocation.External(repoName = label.repoName, relativePath = packagePath)
    }
    else {
      OutputLocation.Workspace(packagePath)
    }
  }

@ApiStatus.Internal
fun BuildTargetInfo.baseDirectory(project: Project): Path? =
  project.targetStorage.resolveExecrootOutputLocation(baseDirectoryLocation)

@ApiStatus.Internal
fun BuildTargetInfo.isWorkspace(repoMapping: RepoMapping): Boolean =
  if (id !is ResolvedLabel || id.isMainWorkspace) {
    true
  }
  else {
    (id as ResolvedLabel).repoName in repoMapping.getLocalRepositories().localRepositories
  }

@get:ApiStatus.Internal
val BuildTarget.baseDirectoryLocation: OutputLocation
  get() = info.baseDirectoryLocation

@ApiStatus.Internal
fun BuildTarget.baseDirectory(project: Project): Path? = info.baseDirectory(project)

@ApiStatus.Internal
fun BuildTarget.isWorkspace(repoMapping: RepoMapping): Boolean = info.isWorkspace(repoMapping)
