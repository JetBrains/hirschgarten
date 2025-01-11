package org.jetbrains.bsp.protocol

data class WorkspaceBazelRepoMappingResult(
  val apparentRepoNameToCanonicalName: Map<String, String>,
  val canonicalRepoNameToPath: Map<String, String>,
)
