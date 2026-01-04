package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import com.intellij.openapi.components.service
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.SyncFlagsService
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.consistentLabels
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

  // TODO: maybe bypass bazel query completely?
  //  this is how set(<file>) is evaluated in bazel - simple walk backwards until package is encountered
  //  https://github.com/bazelbuild/bazel/blob/f11c3eb5c39ef035bfa9593f209b3579df2113aa/src/main/java/com/google/devtools/build/lib/cmdline/TargetPattern.java#L372
  suspend fun resolveSourceFileLabels(ctx: SyncVFSContext, sources: Collection<Path>): Map<Path, Set<Label>> {
    val workspaceRoot = ctx.pathsResolver.workspaceRoot()
    if (ctx.flags.useFastSource2Label) {
      return FastFileToLabelResolver.computeSourceFilesLabels(ctx, sources)
    }

    data class SourceFile(
      val path: Path,
      val repoName: String,
      val repoRoot: Path,
    )

    val sourcesByRepo = sources.filter { it.startsWith(workspaceRoot) }
      .map { source ->
        val (repo, path) = resolveRepositoryFromPath(ctx, source) ?: Pair(null, workspaceRoot)
        SourceFile(
          path = source,
          repoName = repo ?: "",
          repoRoot = path,
        )
      }
      .groupBy { it.repoName }

    val sourceMapping = mutableMapOf<Path, MutableSet<Label>>()
    val connector = ctx.project.service<BazelConnectorService>().ofLegacyTask()
    for ((repo, sources) in sourcesByRepo) {
      val repoRoot = sources.first().repoRoot
      val sourcePaths = sources.filter { it.path.startsWith(repoRoot) }
        .map { it.repoRoot.relativize(it.path) }

      // we end up with set of workspace relative paths
      val query = "set(${sourcePaths.joinToString(separator = " ") { it.toString() }})"
      val result = connector.query(
        startup = {
          overrideWorkspace(repoRoot)
        },
        args = {
          defaults()
          keepGoing()
          consistentLabels()
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
