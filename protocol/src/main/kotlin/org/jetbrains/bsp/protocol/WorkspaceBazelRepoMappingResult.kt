package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class WorkspaceBazelRepoMappingResult(
  val apparentRepoNameToCanonicalName: Map<String, String>,
  val canonicalRepoNameToPath: Map<String, Path>,
)
