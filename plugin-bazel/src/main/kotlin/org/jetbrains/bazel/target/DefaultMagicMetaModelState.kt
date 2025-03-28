package org.jetbrains.bazel.target

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import kotlin.io.path.Path

data class BuildTargetState(
  var id: String = "",
  var displayName: String? = null,
  var kind: TargetKind = TargetKind(kindString = "", languageClasses = emptySet(), ruleType = RuleType.UNKNOWN),
  var tags: List<String> = emptyList(),
  var languageIds: List<String> = emptyList(),
  var baseDirectory: String? = null,
) {
  fun fromState(): BuildTarget =
    BuildTarget(
      id = Label.parse(id),
      kind = kind,
      tags = tags,
      baseDirectory = baseDirectory?.let { Path(it) },
      dependencies = emptyList(),
      sources = emptyList(),
      resources = emptyList(),
    )
}

fun BuildTarget.toState(): BuildTargetState =
  BuildTargetState(
    id = id.toString(),
    displayName = displayName,
    tags = tags,
    baseDirectory = baseDirectory.toString(),
    kind = kind,
  )
