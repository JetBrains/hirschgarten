package org.jetbrains.bazel.sync_new.compat

import org.jetbrains.bazel.sync_new.flow.BzlmodSyncRepoMapping

interface BazelFrontendCompat {
  suspend fun fetchRepoMapping(): BzlmodSyncRepoMapping
}
