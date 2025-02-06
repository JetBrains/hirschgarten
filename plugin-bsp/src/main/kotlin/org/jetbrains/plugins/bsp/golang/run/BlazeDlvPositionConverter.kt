/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.bsp.golang.run

import com.google.idea.blaze.base.ideinfo.ArtifactLocation

internal class BlazeDlvPositionConverter private constructor(
  workspaceRoot: WorkspaceRoot,
  goRoot: String?,
  resolver: ExecutionRootPathResolver,
  remotePaths: MutableSet<String>,
  cgoTrimmedPathsHandler: com.google.idea.blaze.golang.run.BlazeDlvPositionConverter.CgoTrimmedPathsHandler
) : DlvPositionConverter {
  private val root: WorkspaceRoot
  private val goRoot: String?
  private val resolver: ExecutionRootPathResolver
  private val localToRemote: MutableMap<com.intellij.openapi.vfs.VirtualFile?, String?>
  private val normalizedToLocal: MutableMap<String?, com.intellij.openapi.vfs.VirtualFile?>
  private val cgoTrimmedPathsHandler: com.google.idea.blaze.golang.run.BlazeDlvPositionConverter.CgoTrimmedPathsHandler

  init {
    this.root = workspaceRoot
    this.goRoot = goRoot
    this.resolver = resolver
    this.localToRemote =
      com.google.common.collect.Maps.newHashMapWithExpectedSize<com.intellij.openapi.vfs.VirtualFile?, String?>(
        remotePaths.size,
      )
    this.normalizedToLocal =
      com.google.common.collect.Maps.newHashMapWithExpectedSize<String?, com.intellij.openapi.vfs.VirtualFile?>(
        remotePaths.size,
      )
    this.cgoTrimmedPathsHandler = cgoTrimmedPathsHandler

    for (path in remotePaths) {
      val normalized = normalizePath(path)
      if (normalizedToLocal.containsKey(normalized)) {
        continue
      }
      val localFile: com.intellij.openapi.vfs.VirtualFile? = resolve(normalized)
      if (localFile != null) {
        if (remotePaths.contains(normalized!!)) {
          localToRemote.put(localFile, normalized)
        } else {
          localToRemote.put(localFile, path)
        }
        normalizedToLocal.put(normalized, localFile)
      } else {
        com.google.idea.blaze.golang.run.BlazeDlvPositionConverter.Companion.logger.warn("Unable to find local file for debug path: " + path)
      }
    }
  }

  override fun toRemotePath(localFile: com.intellij.openapi.vfs.VirtualFile): String? {
    var remotePath = localToRemote.get(localFile)
    if (remotePath != null) {
      return remotePath
    }
    remotePath =
      if (root.isInWorkspace(localFile))
        root.workspacePathFor(localFile).relativePath()
      else
        localFile.getPath()
    localToRemote.put(localFile, remotePath)
    return remotePath
  }

  override fun toLocalFile(remotePath: String): com.intellij.openapi.vfs.VirtualFile? {
    val normalized = normalizePath(remotePath)
    var localFile: com.intellij.openapi.vfs.VirtualFile? = normalizedToLocal.get(normalized)
    if (localFile == null || !localFile.isValid()) {
      localFile = resolve(normalized)
      if (localFile != null) {
        normalizedToLocal.put(normalized, localFile)
      }
    }
    return localFile
  }

  private fun resolve(normalizedPath: String?): com.intellij.openapi.vfs.VirtualFile? {
    return VfsUtils.resolveVirtualFile(
      resolver.resolveExecutionRootPath(ExecutionRootPath(normalizedPath)),  /* refreshIfNeeded= */
      false,
    )
  }

  private fun normalizePath(path: String): String? {
    if (path.startsWith("/build/work/")) {
      // /build/work/<hash>/<project>/actual/path
      return com.google.idea.blaze.golang.run.BlazeDlvPositionConverter.Companion.afterNthSlash(path, 5)
    } else if (path.startsWith("/tmp/go-build-release/buildroot/")) {
      return com.google.idea.blaze.golang.run.BlazeDlvPositionConverter.Companion.afterNthSlash(path, 4)
    } else if (path.startsWith("GOROOT/")) {
      return goRoot + '/' + com.google.idea.blaze.golang.run.BlazeDlvPositionConverter.Companion.afterNthSlash(
        path,
        1,
      )
    } else if (cgoTrimmedPathsHandler.matchesCgoTrimmedPath(path)) {
      return cgoTrimmedPathsHandler.normalizeCgoTrimmedPath(path)
    }
    return path
  }

  internal class Factory : DlvPositionConverterFactory {
    override fun createPositionConverter(
      project: com.intellij.openapi.project.Project,
      module: com.intellij.openapi.module.Module?,
      remotePaths: MutableSet<String>
    ): DlvPositionConverter? {
      val workspaceRoot: WorkspaceRoot? = WorkspaceRoot.fromProjectSafe(project)
      val goRoot: String = GoSdkService.getInstance(project).getSdk(module).getHomePath()
      val resolver: ExecutionRootPathResolver? = ExecutionRootPathResolver.fromProject(project)
      return if (workspaceRoot != null && resolver != null)
        com.google.idea.blaze.golang.run.BlazeDlvPositionConverter(
          workspaceRoot,
          goRoot,
          resolver,
          remotePaths,
          com.google.idea.blaze.golang.run.BlazeDlvPositionConverter.CgoTrimmedPathsHandler(project, resolver),
        )
      else
        null
    }
  }

  /**
   * This class is responsible for identifying and normalizing paths that may have been trimmed by cgo—a tool in Go for integrating C code.
   *
   *
   * Potential issues addressed:
   *
   *  * Uncertainty about whether sources were trimmed by cgo (the `cgo=true` flag in a Bazel target doesn't guarantee all source paths are trimmed; it depends on whether Go files import C code).
   *  * Ambiguities from name collisions between workspace names and file names.
   *
   *
   *
   * Key functionalities:
   *
   *  * Identify if a path matches a cgo-trimmed path based on Bazel workspace name.
   *  * Normalize cgo-trimmed paths while handling potential collisions.
   *
   *
   *
   * In a rare occasion when multiple source files are detected for the same path, a warning will be shown to user.
   */
  internal class CgoTrimmedPathsHandler(
    project: com.intellij.openapi.project.Project?,
    resolver: ExecutionRootPathResolver
  ) {
    private val project: com.intellij.openapi.project.Project?
    private val cgoSources: MutableSet<String?>
    private val nonCgoSources: MutableSet<String?>
    private val bazelWorkspaceRelativePath: String

    init {
      this.project = project
      val projectData: BlazeProjectData? = BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
      val hasCgoTargets = projectData != null && projectData.getTargetMap().targets().stream()
        .map(TargetIdeInfo::getGoIdeInfo)
        .filter({ obj: Any? -> java.util.Objects.nonNull(obj) })
        .anyMatch(GoIdeInfo::getCgo)

      this.cgoSources =
        if (hasCgoTargets) com.google.idea.blaze.golang.run.BlazeDlvPositionConverter.Companion.collectCgoSources(
          projectData.getTargetMap(),
        ) else kotlin.collections.mutableSetOf<String?>()
      this.nonCgoSources =
        if (hasCgoTargets) com.google.idea.blaze.golang.run.BlazeDlvPositionConverter.Companion.collectNonCgoSources(
          projectData.getTargetMap(),
        ) else kotlin.collections.mutableSetOf<String?>()
      this.bazelWorkspaceRelativePath = resolver.getExecutionRoot().getName() + java.io.File.separator
    }

    fun matchesCgoTrimmedPath(path: String): Boolean {
      return !cgoSources.isEmpty() && path.startsWith(this.bazelWorkspaceRelativePath)
    }

    fun normalizeCgoTrimmedPath(path: String): String? {
      val normalizedPath: String =
        com.google.idea.blaze.golang.run.BlazeDlvPositionConverter.Companion.afterNthSlash(path, 1)
      if (cgoSources.contains(normalizedPath)) {
        if (!nonCgoSources.contains(path) && !cgoSources.contains(path)) {
          return normalizedPath
        } else {
          XDebuggerManagerImpl.getNotificationGroup()
            .createNotification(
              "Multiple source files detected for the same path: " + path + ".\n" +
                "For these source files, breakpoints may not function correctly.\n" +
                "Check for possible collisions between Bazel workspace names and Go package names.",
              com.intellij.openapi.ui.MessageType.WARNING,
            )
            .notify(project)
          return if (nonCgoSources.contains(path)) path else normalizedPath
        }
      } else {
        return path
      }
    }
  }

  companion object {
    private val logger: com.intellij.openapi.diagnostic.Logger =
      com.intellij.openapi.diagnostic.Logger.getInstance(com.google.idea.blaze.golang.run.BlazeDlvPositionConverter::class.java)

    /**
     * @return path substring after nth slash, if path contains at least n slashes, return path
     * unchanged otherwise.
     */
    private fun afterNthSlash(path: String, n: Int): String {
      var index = 0
      for (i in 0..<n) {
        index = path.indexOf('/', index) + 1
        if (index == 0) { // -1 + 1
          return path
        }
      }
      return path.substring(index)
    }

    private fun collectCgoSources(targetMap: TargetMap): MutableSet<String?> {
      return targetMap.targets().stream()
        .map(TargetIdeInfo::getGoIdeInfo)
        .filter({ obj: Any? -> java.util.Objects.nonNull(obj) })
        .filter(GoIdeInfo::getCgo)
        .map(GoIdeInfo::getSources)
        .flatMap({ obj: com.google.common.collect.ImmutableCollection<*>? -> obj.stream() })
        .filter(ArtifactLocation::isMainWorkspaceSourceArtifact)
        .map(ArtifactLocation::getExecutionRootRelativePath)
        .collect(java.util.stream.Collectors.toSet())
    }

    private fun collectNonCgoSources(targetMap: TargetMap): MutableSet<String?> {
      return targetMap.targets().stream()
        .map(TargetIdeInfo::getGoIdeInfo)
        .filter({ obj: Any? -> java.util.Objects.nonNull(obj) })
        .filter({ goIdeInfo -> !goIdeInfo.getCgo() })
        .map(GoIdeInfo::getSources)
        .flatMap({ obj: com.google.common.collect.ImmutableCollection<*>? -> obj.stream() })
        .map(ArtifactLocation::getExecutionRootRelativePath)
        .collect(java.util.stream.Collectors.toSet())
    }
  }
}
