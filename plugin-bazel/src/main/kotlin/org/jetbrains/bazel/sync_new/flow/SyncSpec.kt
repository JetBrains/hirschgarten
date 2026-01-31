package org.jetbrains.bazel.sync_new.flow

data class SyncSpec(
  val skipImplicitFileChanges: Boolean = false,
  val useCachedFile2TargetMapping: Boolean = false,
  val skipUniverseDiff: Boolean = false
)
