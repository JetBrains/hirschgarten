package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bsp.protocol.SourceItem

data class LanguagePluginContext(
  val target: BspTargetInfo.TargetInfo,
  val graph: DependencyGraph,
  val repoMapping: RepoMapping,
  val sources: List<SourceItem>,
  val pathsResolver: BazelPathsResolver,
)
