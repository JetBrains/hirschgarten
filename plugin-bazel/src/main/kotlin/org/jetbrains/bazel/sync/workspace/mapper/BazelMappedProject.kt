package org.jetbrains.bazel.sync.workspace.mapper

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.model.Module

interface BazelMappedProject {
  fun findModule(label: Label): Module?
}
