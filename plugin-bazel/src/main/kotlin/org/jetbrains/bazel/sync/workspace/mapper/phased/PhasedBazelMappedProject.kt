package org.jetbrains.bazel.sync.workspace.mapper.phased

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.BazelMappedProject
import org.jetbrains.bsp.protocol.RawPhasedTarget

data class PhasedBazelMappedProject(val targets: Map<Label, RawPhasedTarget>, val hasError: Boolean) : BazelMappedProject
