package org.jetbrains.bsp.protocol

/**
 * Request and response classes for local->remote resolution.
 */
data class BazelResolveLocalToRemoteParams(val localPaths: List<String>)

data class BazelResolveLocalToRemoteResult(
  /**
   * Maps each local path to the resolved remote path.
   */
  val resolvedPaths: Map<String, String>,
)

/**
 * Request and response classes for remote->local resolution.
 */
data class BazelResolveRemoteToLocalParams(val remotePaths: List<String>, val goRoot: String)

data class BazelResolveRemoteToLocalResult(
  /**
   * Maps each remote path to a local absolute path (or an empty string if not resolvable).
   */
  val resolvedPaths: Map<String, String>,
)
