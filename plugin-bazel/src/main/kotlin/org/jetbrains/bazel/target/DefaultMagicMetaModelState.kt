package org.jetbrains.bazel.target

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities

data class BuildTargetState(
  var id: String = "",
  var displayName: String? = null,
  var capabilities: BuildTargetCapabilities = BuildTargetCapabilities(),
  var tags: List<String> = emptyList(),
  var languageIds: List<String> = emptyList(),
  var baseDirectory: String? = null,
) {
  fun fromState(): BuildTarget =
    BuildTarget(
      id = Label.parse(id),
      capabilities = capabilities,
      tags = tags,
      languageIds = languageIds,
      baseDirectory = baseDirectory,
      dependencies = emptyList(),
      sources = emptyList(),
      resources = emptyList(),
    )
}

fun BuildTarget.toState(): BuildTargetState =
  BuildTargetState(
    id = id.toString(),
    displayName = displayName,
    capabilities = capabilities,
    tags = tags,
    languageIds = languageIds,
    baseDirectory = baseDirectory,
  )
