package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.bazel.sync.workspace.model.LanguageData
import java.nio.file.Path

data class Jdk(val version: String, val javaHome: Path?)

data class JavaModule(
  val jdk: Jdk?,
  val runtimeJdk: Jdk?,
  val javacOpts: List<String>,
  val jvmOps: List<String>,
  val mainOutput: Path,
  val binaryOutputs: List<Path>,
  val mainClass: String?,
  val args: List<String>,
) : LanguageData
