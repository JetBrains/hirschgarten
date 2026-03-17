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
package org.jetbrains.bazel.server.sync.sharding

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.toPath
import org.jetbrains.bsp.protocol.FeatureFlags
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Traverses Bazel packages specified by wildcard target patterns, expanding to a set of
 * single-package target patterns.
 */
internal object PackageLister {
  /**
   * Expands all-in-package-recursive wildcard targets into all-in-single-package targets by
   * traversing the file system, looking for child bazel packages.
   */
  fun expandPackageTargets(
    pathResolver: BazelPathsResolver,
    featureFlags: FeatureFlags,
    wildcardPatterns: List<Label>,
  ): Map<Label, List<Label>> {
    val calculatedBazelSymlinks =
      BazelSymlinksCalculator.calculateBazelSymlinksToExclude(
        pathResolver.workspaceRoot(),
        featureFlags.bazelSymlinksScanMaxDepth,
      )
    return wildcardPatterns
      .filter { it.isRecursive }
      .mapNotNull { pattern ->
        val dir = pathResolver.relativePathToWorkspaceAbsolute(pattern.packagePath.toPath())
        if (!dir.isDirectory()) return@mapNotNull null

        val expandedTargets = mutableListOf<Label>()
        traversePackageRecursively(pathResolver, featureFlags, calculatedBazelSymlinks, mutableListOf(dir), expandedTargets)
        pattern to expandedTargets
      }.toMap()
  }

  private tailrec fun traversePackageRecursively(
    pathResolver: BazelPathsResolver,
    featureFlags: FeatureFlags,
    calculatedBazelSymlinks: Set<Path>,
    dirs: MutableList<Path>,
    output: MutableList<Label>,
  ) {
    val dir = dirs.removeFirstOrNull() ?: return
    if (dir.isEligibleForTraversal(pathResolver, calculatedBazelSymlinks)) {
      val path = pathResolver.getWorkspaceRelativePath(dir)
      if (dir.containsBuildFile()) {
        output.add(Label.parse("//$path:all"))
      }
      val children = dir.toFile().listFiles() ?: arrayOf<File>()
      dirs.addAll(children.asSequence().filter { it.isDirectory }.map { it.toPath() })
    }
    traversePackageRecursively(
      pathResolver,
      featureFlags,
      calculatedBazelSymlinks,
      dirs,
      output,
    )
  }

  private fun Path.isEligibleForTraversal(pathResolver: BazelPathsResolver, calculatedBazelSymlinks: Set<Path>): Boolean =
    !this.isDotBazelBsp(pathResolver) && !calculatedBazelSymlinks.contains(this)

  private fun Path.isDotBazelBsp(pathResolver: BazelPathsResolver): Boolean = this.name == Constants.DOT_BAZELBSP_DIR_NAME && this.parent == pathResolver.workspaceRoot()
  private fun Path.containsBuildFile(): Boolean = Constants.BUILD_FILE_NAMES.any { resolve(it).isRegularFile() }
}
