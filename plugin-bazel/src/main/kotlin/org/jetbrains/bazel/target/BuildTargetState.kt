package org.jetbrains.bazel.target

import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import kotlin.io.path.Path

data class BuildTargetState(
  var id: String = "",
  @get:Attribute(converter = BuildTargetCapabilitiesConverter::class)
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
      baseDirectory = baseDirectory?.let { Path(it) },
      dependencies = emptyList(),
      sources = emptyList(),
      resources = emptyList(),
    )
}

fun BuildTarget.toState(): BuildTargetState =
  BuildTargetState(
    id = id.toString(),
    capabilities = capabilities,
    tags = tags,
    languageIds = languageIds,
    baseDirectory = baseDirectory.toString(),
  )

internal class BuildTargetCapabilitiesConverter : Converter<BuildTargetCapabilities>() {
  override fun fromString(value: String): BuildTargetCapabilities =
    BuildTargetCapabilities(
      canCompile = value.contains("canCompile=true"),
      canTest = value.contains("canTest=true"),
      canRun = value.contains("canRun=true"),
    )

  override fun toString(value: BuildTargetCapabilities): String =
    "canCompile=${value.canCompile},canTest=${value.canTest},canRun=${value.canRun}"
}
