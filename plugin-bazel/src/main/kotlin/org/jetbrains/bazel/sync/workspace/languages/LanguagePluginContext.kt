package org.jetbrains.bazel.sync.workspace.languages

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph

data class LanguagePluginContext(
  val target: BspTargetInfo.TargetInfo,
  val graph: DependencyGraph,
  val repoMapping: RepoMapping,
  val project: Project,
  val pathsResolver: BazelPathsResolver,
)
