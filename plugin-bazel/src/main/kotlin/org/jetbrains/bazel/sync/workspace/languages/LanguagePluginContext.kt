package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph

data class LanguagePluginContext(
  val target: BspTargetInfo.TargetInfo,
  val graph: DependencyGraph
)
