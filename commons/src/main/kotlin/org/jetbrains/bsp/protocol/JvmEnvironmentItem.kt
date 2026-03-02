package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

@ApiStatus.Internal
data class JvmEnvironmentItem(
  val target: Label,
  val classpath: List<Path>,
  val jvmOptions: List<String>,
  val workingDirectory: Path,
  val environmentVariables: Map<String, String>,
  val mainClasses: List<JvmMainClass>? = emptyList(),
)
