package org.jetbrains.bazel.sync.workspace.mapper.normal

import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.model.Library
import org.jetbrains.bazel.sync.workspace.model.Module
import org.jetbrains.bazel.sync.workspace.model.NonModuleTarget

data class AspectBazelMappedProject(
  val workspaceName: String,
  val hasError: Boolean = false,
  val modules: List<Module>,
  val moduleCache: Map<Label, Module>,
  val libraries: Map<Label, Library>,
  val nonModuleTargets: List<NonModuleTarget>, // targets that should be displayed in the project view but are neither modules nor libraries
  val graph: DependencyGraph,
  val repoMapping: RepoMapping
) : BazelMappedProject {
}
