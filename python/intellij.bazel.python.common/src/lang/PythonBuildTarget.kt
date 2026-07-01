package org.jetbrains.bazel.python.lang

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.ClassDiscriminator
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

@ClassDiscriminator(2)
@ApiStatus.Internal
data class PythonBuildTarget(
  val version: String?,
  val interpreter: Path?,
  // imports is the attribute in bazel python rules
  // which specify a list of runfiles relative paths which will be included in PYTHONPATH
  val imports: List<String>,
  // Not used after full sync. Mapping is saved in `PythonResolveIndexService`
  @Transient @JvmField val generatedSources: SourceFileCollection? = null,
  @Transient @JvmField val externalSources: SourceFileCollection? = null,
  val mainFile: Path? = null,
  val mainModule: String? = null,
  val runnerScript: Path? = null,
) : BuildTargetData

@ApiStatus.Internal
fun extractPythonBuildTarget(target: BuildTarget): PythonBuildTarget? = target.extractData()

