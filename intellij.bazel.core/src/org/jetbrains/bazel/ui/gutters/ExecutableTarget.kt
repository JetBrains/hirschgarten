package org.jetbrains.bazel.ui.gutters

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.ExecutableTarget

@ApiStatus.Internal
data class NonImportedExecutableTarget(
  override val id: Label,
  override val kind: TargetKind,
) : ExecutableTarget
