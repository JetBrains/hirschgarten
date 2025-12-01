package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import com.intellij.openapi.components.service
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.overrideWorkspace
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.unwrapProtos
import org.jetbrains.bazel.sync_new.flow.BzlmodSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.DisabledSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark.toPath
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2

// TODO: check if this approach is valid
//  also labels can be resolved on repo basis
//  so go into each repository and call `bazel query "set(<path1> <path2> ...)"`
//  then you will labels relative to repo root
//  the disadvantege of this approach would be that every repo would have its own bazel server running
object SyncVFSLabelResolver {
  // I would use bazel query <file>,
  // but it does not work for files in external repos
  //fun resolveFullLabel(ctx: SyncVFSContext, path: Path): Label? {
  //  val workspaceRoot = ctx.pathsResolver.workspaceRoot()
  //  val workspaceRelativePath = workspaceRoot.relativize(path).parent
  //  return when (ctx.repoMapping) {
  //    is BzlmodSyncRepoMapping -> {
  //      // TODO: profile it
  //      //  if you are a freak and your monorepo has 100s of local modules
  //      //  it will be slow - but there is still room for optimizations
  //      //  (mainly making it at least O(log n), n - number of repos)
  //      val label2Repo = ctx.repoMapping.canonicalToPath
  //        .filter { (_, v) -> path.startsWith(workspaceRoot.resolve(v)) }
  //        .map { (k, v) -> k to v }
  //        .maxByOrNull { (_, v) -> v.toString().length }
  //      if (label2Repo == null) {
  //        return ResolvedLabel(
  //          repo = Main,
  //          packagePath = Package(workspaceRelativePath.map { it.toString() }),
  //          target = SingleTarget(path.fileName.toString()),
  //        )
  //      }
  //      val (label, repoPath) = label2Repo
  //      val repoRelativePath = repoPath.relativize(path.parent)
  //      ResolvedLabel(
  //        repo = Canonical.createCanonicalOrMain(label),
  //        packagePath = Package(repoRelativePath.map { it.toString() }),
  //        target = SingleTarget(path.fileName.toString()),
  //      )
  //    }
  //
  //    DisabledSyncRepoMapping -> {
  //      ResolvedLabel(
  //        repo = Main,
  //        packagePath = Package(workspaceRelativePath.map { it.toString() }),
  //        target = SingleTarget(path.fileName.toString()),
  //      )
  //    }
  //  }
  //}

  fun resolveRepositoryFromPath(ctx: SyncVFSContext, path: Path): Pair<String?, Path>? {
    val workspaceRoot = ctx.pathsResolver.workspaceRoot()
    return when (ctx.repoMapping) {
      is BzlmodSyncRepoMapping -> {
        val (repo, path) = ctx.repoMapping.canonicalToPath
          .filter { (k, _) -> k.isNotEmpty() }
          .filter { (_, v) -> path.startsWith(workspaceRoot.resolve(v)) }
          .map { (k, v) -> k to v }
          .maxByOrNull { (_, v) -> v.toString().length } ?: return null
        repo.ifEmpty { null } to workspaceRoot.resolve(path)
      }

      DisabledSyncRepoMapping -> null
    }
  }

  suspend fun resolveSourceFileLabels(ctx: SyncVFSContext, sources: Collection<Path>): Map<Path, Set<Label>> {
    data class SourceFile(
      val path: Path,
      val repoName: String,
      val repoRoot: Path,
    )

    val workspaceRoot = ctx.pathsResolver.workspaceRoot()

    val sourcesByRepo = sources.map { source ->
      val (repo, path) = resolveRepositoryFromPath(ctx, source) ?: Pair(null, workspaceRoot)
      SourceFile(
        path = source,
        repoName = repo ?: "",
        repoRoot = path,
      )
    }.groupBy { it.repoName }

    val sourceMapping = mutableMapOf<Path, MutableSet<Label>>()
    val connector = ctx.project.service<BazelConnectorService>().ofLegacyTask()
    for ((repo, sources) in sourcesByRepo) {
      val repoRoot = sources.first().repoRoot
      val sourcePaths = sources.map { it.repoRoot.relativize(it.path) }

      // we end up with set of workspace relative paths
      val query = "set(${sourcePaths.joinToString(separator = " ") { it.toString() }})"
      val result = connector.query(
        startup = {
          overrideWorkspace(repoRoot)
        },
        args = {
          defaults()
          keepGoing()
          output(QueryOutput.PROTO)
          query(query)
        },
      )
      result.unwrap().unwrapProtos()
        .filter { it.hasSourceFile() }
        .map { it.sourceFile }
        .forEach { file ->
          val label = Label.parse(file.name)
            .assumeResolved().copy(repo = Canonical.createCanonicalOrMain(repo))
          val path = file.toPath()
          sourceMapping.getOrPut(path) { mutableSetOf() }.add(label)
        }
    }

    return sourceMapping
  }

}
