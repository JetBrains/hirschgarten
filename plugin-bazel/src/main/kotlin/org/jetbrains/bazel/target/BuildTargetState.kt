package org.jetbrains.bazel.target

import com.google.gson.Gson
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import kotlin.io.path.Path

data class BuildTargetState(
  var id: String = "",
  var kind: String = "",
  var tags: List<String> = emptyList(),
  var languageIds: List<String> = emptyList(),
  var baseDirectory: String = "",
  var
) {
  fun fromState(): BuildTarget =
    BuildTarget(
      id = Label.parse(id),
      kind = Gson().fromJson(kind, TargetKind::class.java),
      tags = tags,
      languageIds = languageIds,
      baseDirectory = Path(baseDirectory),
      dependencies = emptyList(),
      sources = emptyList(),
      resources = emptyList(),
    )
}

fun BuildTarget.toState(): BuildTargetState =
  BuildTargetState(
    id = id.toString(),
    kind = Gson().toJson(kind),
    tags = tags,
    languageIds = languageIds,
    baseDirectory = baseDirectory.toString(),
  )
