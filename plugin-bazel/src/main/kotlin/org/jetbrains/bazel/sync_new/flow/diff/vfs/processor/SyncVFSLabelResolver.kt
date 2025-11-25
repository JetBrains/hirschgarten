package org.jetbrains.bazel.sync_new.flow.diff.vfs.processor

import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.sync_new.flow.BzlmodSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.DisabledSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.diff.vfs.SyncVFSContext
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2

object SyncVFSLabelResolver {
  // I would use bazel query <file>,
  // but it does not work for files in external repos
  fun resolveFullLabel(ctx: SyncVFSContext, path: Path): Label? {
    val workspaceRoot = ctx.pathsResolver.workspaceRoot()
    val workspaceRelativePath = workspaceRoot.relativize(path).parent
    return when (ctx.repoMapping) {
      is BzlmodSyncRepoMapping -> {
        // TODO: profile it
        //  if you are a freak and your monorepo has 100s of local modules
        //  it will be slow
        val label2Repo = ctx.repoMapping.canonicalToPath
          .filter { (_, v) -> path.startsWith(workspaceRoot.resolve(v)) }
          .map { (k, v) -> k to v }
          .maxByOrNull { (_, v) -> v.toString().length }
        if (label2Repo == null) {
          return ResolvedLabel(
            repo = Main,
            packagePath = Package(workspaceRelativePath.map { it.toString() }),
            target = AllRuleTargets,
          )
        }
        val (label, repoPath) = label2Repo
        val repoRelativePath = repoPath.relativize(path.parent)
        ResolvedLabel(
          repo = Canonical.createCanonicalOrMain(label),
          packagePath = Package(repoRelativePath.map { it.toString() }),
          target = AllRuleTargets,
        )
      }

      DisabledSyncRepoMapping -> {
        ResolvedLabel(
          repo = Main,
          packagePath = Package(workspaceRelativePath.map { it.toString() }),
          target = AllRuleTargets,
        )
      }
    }
  }

}
