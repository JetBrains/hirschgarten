package org.jetbrains.bsp.bazel.server.sync.languages.go

import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.server.model.LanguageData
import java.net.URI

data class GoModule(
  val sdkHomePath: URI?,
  val importPath: String?,
  val generatedSources: List<URI>,
  val generatedLibraries: List<URI>,
  val ruleKind: String,
  val libraryLabels: List<Label>,
) : LanguageData
