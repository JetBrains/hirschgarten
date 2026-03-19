package org.jetbrains.bazel.sync.workspace.model

import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

internal data class NonModuleTarget(
  val label: Label,
  val targetKind: TargetKind,
  val tags: Set<String>,
  val baseDirectory: Path,
  val generatorName: String?,
)
