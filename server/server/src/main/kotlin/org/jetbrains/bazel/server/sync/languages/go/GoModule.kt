package org.jetbrains.bazel.server.sync.languages.go

import org.jetbrains.bazel.server.model.LanguageData
import java.nio.file.Path

data class GoModule(
  val sdkHomePath: Path?,
  val importPath: String,
  val generatedSources: List<Path>,
  val generatedLibraries: List<Path>,
) : LanguageData
