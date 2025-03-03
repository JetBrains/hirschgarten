package org.jetbrains.bazel.server.sync.languages.go

import org.jetbrains.bazel.server.model.LanguageData
import java.net.URI

data class GoModule(
  val sdkHomePath: URI?,
  val importPath: String,
  val generatedSources: List<URI>,
  val generatedLibraries: List<URI>,
) : LanguageData
