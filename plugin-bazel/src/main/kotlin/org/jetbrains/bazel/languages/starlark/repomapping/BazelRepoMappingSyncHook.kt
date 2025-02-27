package org.jetbrains.bazel.languages.starlark.repomapping

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.utils.safeCastToURI
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

val Project.apparentRepoNameToCanonicalName: Map<String, String>
  get() = BazelRepoMappingService.getInstance(this).apparentRepoNameToCanonicalName

val Project.canonicalRepoNameToApparentName: Map<String, String>
  get() = BazelRepoMappingService.getInstance(this).canonicalRepoNameToApparentName

val Project.canonicalRepoNameToPath: Map<String, Path>
  get() = BazelRepoMappingService.getInstance(this).canonicalRepoNameToPath

val Project.repositoryPaths: Set<Path>
  get() = BazelRepoMappingService.getInstance(this).repositoryPaths

class BazelRepoMappingSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val bazelRepoMappingService = BazelRepoMappingService.getInstance(environment.project)
    val bazelRepoMappingResult =
      query("workspace/bazelRepoMapping") {
        environment.server.workspaceBazelRepoMapping()
      }
    bazelRepoMappingService.apparentRepoNameToCanonicalName = bazelRepoMappingResult.apparentRepoNameToCanonicalName
    bazelRepoMappingService.canonicalRepoNameToPath =
      bazelRepoMappingResult.canonicalRepoNameToPath.mapValues { (_, uri) -> uri.safeCastToURI().toPath() }
  }
}

internal data class BazelRepoMappingServiceState(
  var apparentRepoNameToCanonicalName: Map<String, String> = emptyMap(),
  var canonicalRepoNameToPath: Map<String, String> = emptyMap(),
)

@State(
  name = "BazelRepoMappingService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
internal class BazelRepoMappingService : PersistentStateComponent<BazelRepoMappingServiceState> {
  @Volatile
  internal var apparentRepoNameToCanonicalName: Map<String, String> = emptyMap()
    set(value) {
      field = value
      canonicalRepoNameToApparentName = value.entries.associate { (apparent, canonical) -> canonical to apparent }
    }

  @Volatile
  internal var canonicalRepoNameToApparentName: Map<String, String> = emptyMap()

  @Volatile
  internal var canonicalRepoNameToPath: Map<String, Path> = emptyMap()
    set(value) {
      field = value
      repositoryPaths = canonicalRepoNameToPath.values.toSet()
    }

  @Volatile
  internal var repositoryPaths: Set<Path> = emptySet()

  override fun getState(): BazelRepoMappingServiceState? =
    BazelRepoMappingServiceState(
      apparentRepoNameToCanonicalName,
      canonicalRepoNameToPath.mapValues { (_, path) -> path.toUri().toString() },
    )

  override fun loadState(state: BazelRepoMappingServiceState) {
    apparentRepoNameToCanonicalName = state.apparentRepoNameToCanonicalName
    canonicalRepoNameToPath = state.canonicalRepoNameToPath.mapValues { (_, uri) -> URI.create(uri).toPath() }
  }

  companion object {
    internal fun getInstance(project: Project): BazelRepoMappingService = project.service<BazelRepoMappingService>()
  }
}
