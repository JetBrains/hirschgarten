package org.jetbrains.bazel.ignore

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.rethrowControlFlowException
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.environment.BazelProjectContextService
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.workspace.canonicalRepoNameToPath
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.relativeToOrNull

@ApiStatus.Internal
class DefaultBazelIgnoreService(val project: Project): BazelIgnoreService {
  // We are not interested in live updates since Bazel uses ignore only during sync
  private val state = AtomicReference<State?>(null)

  override fun isIgnored(path: Path): Boolean {
    val state = getOrUpdate() ?: return false

    val absolutePath = if (path.isAbsolute) {
      if (!path.startsWith(state.workspaceRoot))
        return false
      path
    } else {
      state.workspaceRoot.resolve(path)
    }

    val matchers = state.matchesFor(absolutePath) ?: return false
    val relativePath = absolutePath.relativeToOrNull(matchers.first) ?: return false
    return matchers.second.any { it.match(relativePath) }
  }

  private fun getOrUpdate(): State? {
    val state = this.state.get()
    if (state != null)
      return state

    val projectRootDir = project.service<BazelProjectContextService>().projectRootDir ?: return null
    val newState = scan(Path.of(projectRootDir.path), project.canonicalRepoNameToPath.values)
    if (this.state.compareAndSet(null, newState)) {
      return newState
    }

    return this.state.get()
  }

  fun update (workspaceRoot: Path, repoMapping: RepoMapping): State {
    val state = scan(workspaceRoot, repoMapping.getLocalRepositories().localRepositories.values)
    this.state.set(state)
    return state
  }

  private fun scan(workspaceRoot: Path, localRepos: Collection<Path>): State {
    val errors = ArrayList<Path>()
    val patterns = buildMap {
      put(workspaceRoot, scanRepoRoot(workspaceRoot, errors))
      localRepos.forEach { localRepo ->
        val path = workspaceRoot.resolve(localRepo)
        put(path, scanRepoRoot(path, errors))
      }
    }

    return State(
      workspaceRoot = workspaceRoot,
      patterns = patterns,
      errorsParsing = errors,
    )
  }

  private fun scanRepoRoot(path: Path, errors: MutableList<Path>): List<BazelIgnoreMatcher> {
    val matchers = ArrayList<BazelIgnoreMatcher>()

    val bazelIgnore = path.resolve(Constants.BAZEL_IGNORE_FILE_NAME)
    if (bazelIgnore.exists()) {
      try {
        matchers.add(BazelIgnoreMatcherFactory.fromBazelIgnoreFile(bazelIgnore.readText(Charsets.UTF_8)))
      } catch (ex: Throwable) {
        rethrowControlFlowException(ex)
        errors.add(path)
        log.error("Error scanning file $bazelIgnore", ex)
      }
    }

    val repoBazel = path.resolve(Constants.REPO_BAZEL_FILE_NAME)
    if (repoBazel.exists()) {
      try {
        matchers.add(BazelIgnoreMatcherFactory.fromRepoBazelFile(repoBazel.readText(Charsets.UTF_8)))
      } catch (ex: Throwable) {
        rethrowControlFlowException(ex)
        errors.add(path)
        log.error("Error scanning file $repoBazel", ex)
      }
    }

    return matchers
  }

  private fun State.matchesFor(path: Path): Pair<Path, List<BazelIgnoreMatcher>>? {
    var p: Path? = path
    while (p != null) {
      val machers = patterns[p]
      if (machers != null)
        return p to machers
      p = p.parent
    }
    return null
  }

  class State(
    val workspaceRoot: Path,
    val patterns: Map<Path, List<BazelIgnoreMatcher>>,
    val errorsParsing: List<Path>,
  )

  companion object {
    val log = logger<DefaultBazelIgnoreService>()
  }
}

internal class UpdateBazelIgnoreSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Update Bazel ignore files") { taskId ->
      val service = BazelIgnoreService.getInstance(environment.project) as? DefaultBazelIgnoreService
      if (service != null) {
        val state = service.update(
          environment.server.bazelInfo.workspaceRoot,
          environment.workspace.repoMapping,
        )
        state.errorsParsing.forEach { path ->
          environment.project.syncConsole.addDiagnosticMessage(
            taskId,
            path = path, line = 0, column = 0,
            message = "Error parsing ${path.fileName}",
            description = null,
            severity = MessageEvent.Kind.ERROR,
          )
        }
      }
    }
  }
}
