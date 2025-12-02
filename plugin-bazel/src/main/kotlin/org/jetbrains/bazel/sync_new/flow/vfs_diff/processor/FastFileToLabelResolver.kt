package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import com.google.common.base.Joiner
import com.google.common.base.Splitter
import it.unimi.dsi.fastutil.objects.Object2BooleanMap
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import java.nio.file.Files
import java.nio.file.Path

object FastFileToLabelResolver {
  private val SLASH_SPLITTER = Splitter.on('/')
  private val SLASH_JOINER = Joiner.on('/')

  // simplified version of
  // https://github.com/bazelbuild/bazel/blob/f11c3eb5c39ef035bfa9593f209b3579df2113aa/src/main/java/com/google/devtools/build/lib/cmdline/TargetPattern.java#L372
  // TODO: correctly handle bazel ignored packages
  fun computeSourceFilesLabels(ctx: SyncVFSContext, sources: Collection<Path>): Map<Path, Set<Label>> {
    val packageCache = Object2BooleanOpenHashMap<Path>()
    val labels = mutableMapOf<Path, MutableSet<Label>>()

    val workspaceRoot = ctx.pathsResolver.workspaceRoot()
    for (source in sources.filter { it.startsWith(workspaceRoot) }) {
      val (repo, repoPath) = SyncVFSLabelResolver.resolveRepositoryFromPath(ctx, source) ?: Pair(null, workspaceRoot)
      val repoRelativePath = repoPath.relativize(source)
      val segments = SLASH_SPLITTER.splitToList(repoRelativePath.toString())

      for (n in segments.size - 1 downTo 0) {
        val pkg = segments.subList(0, n)
        val pkgPath = repoPath.resolve(SLASH_JOINER.join(pkg))
        if (isPackage(packageCache, pkgPath)) {
          val targetName = SLASH_JOINER.join(segments.subList(n, segments.size))
          val label = ResolvedLabel(
            repo = Canonical.createCanonicalOrMain(repo ?: ""),
            packagePath = Package(pkg),
            target = SingleTarget(targetName)
          )
          labels.getOrPut(source) { mutableSetOf() }.add(label)
          break
        }
      }
    }
    return labels
  }

  private fun isPackage(cache: Object2BooleanMap<Path>, path: Path) =
    cache.computeIfAbsent(path, FastFileToLabelResolver::isPackageUncached)

  private fun isPackageUncached(path: Path): Boolean {
    if (!Files.isDirectory(path)) {
      return false
    }

    return Files.exists(path.resolve("BUILD"))
      || Files.exists(path.resolve("BUILD.bazel"))
  }
}
