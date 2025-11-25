package org.jetbrains.bazel.sync_new.flow.diff.vfs

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.sync_new.flow.SyncRepoMapping

class SyncVFSContext(
  val project: Project,
  val storage: SyncVFSStoreService,
  val repoMapping: SyncRepoMapping,
  val pathsResolver: BazelPathsResolver,
)
