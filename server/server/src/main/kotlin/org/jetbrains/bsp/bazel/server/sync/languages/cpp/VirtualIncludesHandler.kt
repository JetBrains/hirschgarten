/*
 * Copyright 201<insert year> The Bazel Authors. All rights reserved.
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

// Copyright 2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bsp.bazel.server.sync.languages.cpp

import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bazel.commons.label.ResolvedLabel
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.CppPathResolver.CppPathResolver.getFileRootedAt
import java.net.URI
import java.nio.file.Path

/**
 * Utility class designed to convert execroot `_virtual_includes` references to
 * either external or local workspace.
 *
 *
 * Virtual includes are generated for targets with strip_include_prefix attribute
 * and are stored for external workspaces in
 *
 *
 * `bazel-out/.../bin/external/.../_virtual_includes/...`
 *
 *
 * or for local workspace in
 *
 *
 * `bazel-out/.../bin/.../_virtual_includes/...`
 */
class VirtualIncludesHandler(val pathResolver: CppPathResolver) {
  val virtualIncludeDirectory: Path = Path.of("_virtual_includes")

  private val externalDirectoryIdx: Int = 3
  private val externalWorkspaceIdx: Int = 4
  private val workspacePathStartForExternalWorkspace: Int = 5
  private val workspacePathStartForLocalWorkspace: Int = 3

  /**
   * Resolves execution root path to `_virtual_includes` directory to the matching workspace
   * location
   *
   * @return list of resolved paths if required information is obtained from execution root path and
   * target data or empty list if resolution has failed
   */
  fun resolveVirtualInclude(
    path: Path,
    externalWorkspacePath: Path,
    targetMap: Map<String, TargetInfo>,
  ): List<URI> {
    val key =
      try {
        guessTargetKey(path)
      } catch (exception: java.lang.IndexOutOfBoundsException) {
        return emptyList()
      }
    if (key == null) {
      return emptyList()
    }

    val info = targetMap[key] ?: return emptyList()

    if (info.sourcesList.any { !it.isSource }) {
      // target contains generated sources which cannot be found in the project root, fallback to virtual include directory
      return emptyList()
    }

    val cIdeInfo = info.cppTargetInfo ?: return emptyList()

    if (!cIdeInfo.getIncludePrefix().isEmpty()) {
      // it is not possible to handle include prefixes here, fallback to virtual include directory
      return emptyList()
    }

    var stripPrefix = cIdeInfo.stripIncludePrefix
    if (stripPrefix == null || stripPrefix.isBlank()) {
      // virtual header can only be generated when stripPrefix is present
      return emptyList()
    }

    // strip prefix is a path not a label, `//something` is invalid
    stripPrefix = stripPrefix.replace("/+".toRegex(), "/")
    // remove trailing slash
    if (stripPrefix.endsWith("/")) {
      stripPrefix = stripPrefix.substring(0, stripPrefix.length - 1)
    }
    val keyLabel = Label.parse(key)
    val workspacePath =
      if (stripPrefix.startsWith("/")) {
        Path.of(stripPrefix.substring(1))
      } else {
        Path.of(keyLabel.packagePath.toString()).resolve(stripPrefix)
      }

    // if this header is not external, then call now we can resolveToIncludeDirectories on it
    if (keyLabel.isMainWorkspace) {
      return pathResolver.resolveToIncludeDirectories(workspacePath, targetMap)
    }

    val externalRoot =
      Path
        .of("external")
        .resolve((keyLabel as ResolvedLabel).repo.repoName)
        .resolve(workspacePath)
    return listOf(externalRoot.getFileRootedAt(externalWorkspacePath).toUri())
  }

  /**
   * @throws IndexOutOfBoundsException if executionRootPath has _virtual_includes but its content is
   * unexpected
   */
  private fun guessTargetKey(path: Path): String? {
    val split = path.toList()
    val virtualIncludesIdx: Int = split.indexOf(virtualIncludeDirectory)

    if (virtualIncludesIdx > -1) {
      val externalWorkspaceName: String? =
        if (split[externalDirectoryIdx] == Path.of("external")) split[externalWorkspaceIdx].toString() else null

      val workspacePathStart: Int =
        if (externalWorkspaceName != null) workspacePathStartForExternalWorkspace else workspacePathStartForLocalWorkspace

      val workspacePaths: List<Path> =
        if (workspacePathStart < virtualIncludesIdx) {
          split.subList(workspacePathStart, virtualIncludesIdx)
        } else {
          listOf(Path.of(""))
        }

      val workspacePathString: String =
        workspacePaths
          .reduce { obj: Path, other: Path? -> obj.resolve(other) }
          .toString()

      val target = split[virtualIncludesIdx + 1].toString()
      val workspacePath = Path.of(workspacePathString) ?: return null

      return createLabelName(externalWorkspaceName, workspacePath, target)
    }
    return null
  }

  fun createLabelName(
    externalWorkspaceName: String?,
    packagePath: Path,
    targetName: String,
  ): String = "${if (externalWorkspaceName != null) "@$externalWorkspaceName" else ""}//$packagePath:$targetName"
}
