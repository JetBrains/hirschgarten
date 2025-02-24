package org.jetbrains.bsp.bazel.server.model

import java.net.URI

data class SourceWithData(val source: URI, val jvmPackagePrefix: String? = null)

data class SourceSet(
  val sources: Set<SourceWithData>,
  val generatedSources: Set<SourceWithData>,
  val sourceRoots: Set<URI>,
)
