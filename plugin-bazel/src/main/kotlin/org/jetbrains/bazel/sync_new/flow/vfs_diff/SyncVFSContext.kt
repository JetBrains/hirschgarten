package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.sync_new.flow.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.SyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.SyncScope
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseDiff
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseState

class SyncVFSContext(
  val project: Project,
  val storage: SyncVFSStoreService,
  val repoMapping: SyncRepoMapping,
  val pathsResolver: BazelPathsResolver,
  val scope: SyncScope,
  val isFirstSync: Boolean,
  val universeDiff: SyncColdDiff,
)
