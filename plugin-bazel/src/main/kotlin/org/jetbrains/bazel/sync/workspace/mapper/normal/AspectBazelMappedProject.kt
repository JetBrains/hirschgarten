package org.jetbrains.bazel.sync.workspace.mapper.normal

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.model.GoLibrary
import org.jetbrains.bazel.sync.workspace.model.Library
import org.jetbrains.bazel.sync.workspace.model.Module
import org.jetbrains.bazel.sync.workspace.model.NonModuleTarget

data class AspectBazelMappedProject(
  val workspaceName: String,
  val hasError: Boolean = false,
  val modules: List<Module>,
  val moduleCache: Map<Label, Module>,
  val libraries: Map<Label, Library>,
  val goLibraries: Map<Label, GoLibrary>,
  val nonModuleTargets: List<NonModuleTarget>, // targets that should be displayed in the project view but are neither modules nor libraries
) : BazelMappedProject {
  override fun findModule(label: Label): Module? = moduleCache[label]
}
