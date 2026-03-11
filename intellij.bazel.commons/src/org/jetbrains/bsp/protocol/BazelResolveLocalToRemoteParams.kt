package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

/**
 * Request and response classes for local->remote resolution.
 */
@ApiStatus.Internal
data class BazelResolveLocalToRemoteParams(val localPaths: List<String>)

@ApiStatus.Internal
data class BazelResolveLocalToRemoteResult(
  /**
   * Maps each local path to the resolved remote path.
   */
  val resolvedPaths: Map<String, String>,
)

/**
 * Request and response classes for remote->local resolution.
 */
@ApiStatus.Internal
data class BazelResolveRemoteToLocalParams(val remotePaths: List<String>, val goRoot: String)

@ApiStatus.Internal
data class BazelResolveRemoteToLocalResult(
  /**
   * Maps each remote path to a local absolute path (or an empty string if not resolvable).
   */
  val resolvedPaths: Map<String, String>,
)
