package org.jetbrains.bsp.bazel.server.model

import org.jetbrains.bazel.commons.label.Label
import java.net.URI

data class NonModuleTarget(
  val label: Label,
  val languages: Set<Language>,
  val tags: Set<Tag>,
  val baseDirectory: URI,
)
