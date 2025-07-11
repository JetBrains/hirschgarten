package org.jetbrains.bazel.server.sync.languages.python

import org.jetbrains.bazel.server.model.LanguageData
import java.nio.file.Path

data class PythonModule(
  val interpreter: Path?,
  val version: String?,
  val imports: List<String>,
  val isCodeGenerator: Boolean,
  val generatedSources: List<Path>,
) : LanguageData
