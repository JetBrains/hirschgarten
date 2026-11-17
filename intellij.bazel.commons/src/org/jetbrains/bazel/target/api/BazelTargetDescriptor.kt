package org.jetbrains.bazel.target.api

import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label

interface BazelTargetDescriptor {
  val label: Label
  val kind: TargetKind
}
