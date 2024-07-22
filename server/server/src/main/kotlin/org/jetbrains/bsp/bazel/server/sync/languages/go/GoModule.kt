package org.jetbrains.bsp.bazel.server.sync.languages.go

import org.jetbrains.bsp.bazel.server.model.LanguageData
import java.net.URI

data class GoModule (
  val sdkHomePath: URI?,
  val importPath: String,
  val origSrcs: List<URI>
): LanguageData
