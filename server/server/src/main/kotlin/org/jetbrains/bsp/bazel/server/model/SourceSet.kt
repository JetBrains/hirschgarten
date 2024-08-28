package org.jetbrains.bsp.bazel.server.model

import java.net.URI

data class SourceSet(
  val sources: Set<SourceWithData>,
  val generatedSources: Set<SourceWithData>,
  val sourceRoots: Set<URI>,
)

data class SourceWithData(val source: URI, val data: Any? = null)
