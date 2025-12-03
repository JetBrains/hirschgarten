package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import org.jetbrains.bazel.sync_new.flow.BzlmodSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.DisabledSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import java.nio.file.Path

object WorkspacePathUtils {
  fun resolveExternalRepoPath(ctx: SyncVFSContext, path: Path): Path {
    val externalReposPath = ctx.pathsResolver.outputBase().resolve("external")
    if (!path.startsWith(externalReposPath)) {
      return path
    }
    val externalRelativePath = externalReposPath.relativize(path)
    val repoName = externalRelativePath.first().toString()
    val repoRelativePath = externalRelativePath.subpath(1, externalRelativePath.nameCount)
    return when (ctx.repoMapping) {
      is BzlmodSyncRepoMapping -> {
        val repoPath = ctx.repoMapping.canonicalToPath[repoName] ?: path
        repoPath.resolve(repoRelativePath)
      }
      DisabledSyncRepoMapping -> path
    }
  }
}
