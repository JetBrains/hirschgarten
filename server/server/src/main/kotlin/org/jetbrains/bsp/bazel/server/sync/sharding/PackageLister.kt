/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bsp.bazel.server.sync.sharding

import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.utils.BazelSymlinksCalculator
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

/**
 * Traverses Bazel packages specified by wildcard target patterns, expanding to a set of
 * single-package target patterns.
 */
internal object PackageLister {
  /**
   * Expands all-in-package-recursive wildcard targets into all-in-single-package targets by
   * traversing the file system, looking for child blaze packages.
   *
   *
   * Returns null if directory traversal failed or was cancelled.
   */
  fun expandPackageTargets(
    pathResolver: BazelPathsResolver,
    bazelInfo: BazelInfo,
    featureFlags: FeatureFlags,
    wildcardPatterns: List<Label>,
  ): Map<Label, List<Label>> {
    val res = mutableMapOf<Label, List<Label>>()
    for (pattern in wildcardPatterns) {
      if (!pattern.isRecursive) {
        continue
      }
      val dir = pathResolver.relativePathToWorkspaceAbsolute(pattern.packagePath)
      if (!dir.isDirectory()) {
        continue
      }
      val expandedTargets = mutableListOf<Label>()
      traversePackageRecursively(
        pathResolver,
        bazelInfo,
        featureFlags,
        dir,
        expandedTargets,
      )
      res[pattern] = expandedTargets
    }
    return res
  }

  private fun traversePackageRecursively(
    pathResolver: BazelPathsResolver,
    bazelInfo: BazelInfo,
    featureFlags: FeatureFlags,
    dir: Path,
    output: MutableList<Label>,
  ) {
    if (!dir.isEligibleForTraversal(bazelInfo, featureFlags)) return
    val path = pathResolver.getWorkspaceRelativePath(dir)
    if (dir.containsBuildFile()) {
      output.add(Label.allFromPackageNonRecursive(path))
    }
    val children = dir.toFile().listFiles() ?: return
    for (child in children) {
      if (child.isDirectory) {
        traversePackageRecursively(
          pathResolver,
          bazelInfo,
          featureFlags,
          child.toPath(),
          output,
        )
      }
    }
  }

  fun Path.isEligibleForTraversal(bazelInfo: BazelInfo, featureFlags: FeatureFlags): Boolean {
    if (this == bazelInfo.dotBazelBsp()) return false
    val bazelSymlinks = BazelSymlinksCalculator.getBazelSymlinksToExclude(bazelInfo.workspaceRoot, featureFlags.bazelSymlinksScanMaxDepth)
    if (bazelSymlinks.any { it == this }) return false
    return true
  }

  fun Path.containsBuildFile(): Boolean {
    val possibleBuildFileNames = listOf("BUILD", "BUILD.bazel")
    return possibleBuildFileNames.any { resolve(it).isRegularFile() }
  }
}
