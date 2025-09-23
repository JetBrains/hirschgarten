package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.ResolvedLabel

/**
 * Determines if a target label is internal to the workspace.
 *
 * A target is considered internal if:
 * - It belongs to the main workspace
 * - It's Gazelle-generated
 * - In bzlmod mode, it's in a local repository mapped in the repo mapping
 */
fun isInternalTarget(label: ResolvedLabel, repoMapping: RepoMapping): Boolean =
  label.isMainWorkspace || label.isGazelleGenerated || (
    repoMapping is BzlmodRepoMapping &&
    repoMapping.canonicalRepoNameToLocalPath.containsKey(label.repo.repoName)
  )

/**
 * Checks if a target has any sources with the specified file extensions.
 *
 * @param targetInfo The target to check
 * @param extensions File extensions to look for (e.g., ".java", ".go", ".py")
 * @return true if at least one source file has one of the specified extensions
 */
fun hasSourcesWithExtensions(targetInfo: BspTargetInfo.TargetInfo, vararg extensions: String): Boolean =
  targetInfo.sourcesList.any { source ->
    extensions.any { ext -> source.relativePath.endsWith(ext) }
  }