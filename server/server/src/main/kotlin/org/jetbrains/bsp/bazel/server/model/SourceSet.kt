package org.jetbrains.bsp.bazel.server.model

import org.jetbrains.bsp.protocol.EnhancedSourceItemData
import java.net.URI

data class SourceWithData(val source: URI, val data: EnhancedSourceItemData? = null)

data class SourceSet(
  val sources: Set<SourceWithData>,
  val generatedSources: Set<SourceWithData>,
  val sourceRoots: Set<URI>,
)
