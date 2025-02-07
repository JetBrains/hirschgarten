package org.jetbrains.bazel.languages.starlark.repomapping

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.plugins.bsp.impl.flow.sync.queryIf
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

val Project.apparentRepoNameToCanonicalName: Map<String, String>
  get() = BazelRepoMappingService.getInstance(this).apparentRepoNameToCanonicalName

val Project.canonicalRepoNameToPath: Map<String, Path>
  get() = BazelRepoMappingService.getInstance(this).canonicalRepoNameToPath

val Project.repositoryPaths: Set<Path>
  get() = BazelRepoMappingService.getInstance(this).repositoryPaths

class BazelRepoMappingSyncHook : ProjectSyncHook {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) =
    coroutineScope {
      val bazelRepoMappingService = BazelRepoMappingService.getInstance(environment.project)
      val bazelRepoMappingResult =
        queryIf(environment.capabilities.bazelRepoMappingProvider, "workspace/bazelRepoMapping") {
          environment.server.workspaceBazelRepoMapping()
        } ?: WorkspaceBazelRepoMappingResult(emptyMap(), emptyMap())
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
  internal var apparentRepoNameToCanonicalName: Map<String, String> = emptyMap()
  internal var canonicalRepoNameToPath: Map<String, Path> = emptyMap()
    set(value) {
      field = value
      repositoryPaths = canonicalRepoNameToPath.values.toSet()
    }
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
