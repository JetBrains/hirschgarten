package org.jetbrains.bazel.python.lang

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

@ApiStatus.Internal
data class PythonBuildTarget(
  val version: String?,
  val interpreter: OutputLocation?,
  // imports is the attribute in bazel python rules
  // which specify a list of runfiles relative paths which will be included in PYTHONPATH
  val imports: List<String>,
  // Not used after full sync. Mapping is saved in `PythonResolveIndexService`
  val generatedSources: OutputLocationCollection = OutputLocationCollection.EMPTY,
  val externalSources: OutputLocationCollection = OutputLocationCollection.EMPTY,
  val mainFile: Path? = null,
  val mainModule: String? = null,
  val runnerScript: Path? = null,
  val targetArgs: List<String> = emptyList(),
) : BuildTargetData

@ApiStatus.Internal
fun extractPythonBuildTarget(target: BuildTarget): PythonBuildTarget? = target.extractData()
