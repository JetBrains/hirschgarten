package org.jetbrains.bazel.sync.workspace.mapper.phased

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.model.Module
import org.jetbrains.bsp.protocol.RawPhasedTarget

data class PhasedBazelMappedProject(val targets: Map<Label, RawPhasedTarget>, val hasError: Boolean) : BazelMappedProject {
  // idea of phased project is to have limited project information
  override fun findModule(label: Label): Module? = null
}
