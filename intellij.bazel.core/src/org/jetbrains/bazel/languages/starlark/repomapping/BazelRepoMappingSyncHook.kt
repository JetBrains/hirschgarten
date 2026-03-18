package org.jetbrains.bazel.languages.starlark.repomapping

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.workspace.BazelRepoMappingService
import java.nio.file.Path
import kotlin.io.path.Path

@TestOnly
@ApiStatus.Internal
fun Project.injectCanonicalRepoNameToPath(canonicalRepoNameToPath: Map<String, Path>) {
  val service = PersistentBazelRepoMappingService.getInstance(this)
  service.canonicalRepoNameToPath = canonicalRepoNameToPath
}

@TestOnly
@ApiStatus.Internal
fun Project.injectCanonicalRepoNameToApparentName(canonicalRepoNameToApparentName: Map<String, String>) {
  val service = PersistentBazelRepoMappingService.getInstance(this)
  service.canonicalRepoNameToApparentName = canonicalRepoNameToApparentName
}

internal class BazelRepoMappingSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Load bazel repo mapping") {
      val bazelRepoMappingService = PersistentBazelRepoMappingService.getInstance(environment.project)
      val bazelRepoMappingResult = environment.server.workspaceBazelRepoMapping(environment.taskId)
      when (val mapping = bazelRepoMappingResult.repoMapping) {
        is BzlmodRepoMapping -> {
          bazelRepoMappingService.apparentRepoNameToCanonicalName = mapping.apparentRepoNameToCanonicalName
          bazelRepoMappingService.canonicalRepoNameToPath = mapping.canonicalRepoNameToPath
        }
        else -> {}
      }
    }
  }
}

@VisibleForTesting
@ApiStatus.Internal
data class BazelRepoMappingServiceState(
  var apparentRepoNameToCanonicalName: Map<String, String> = emptyMap(),
  var canonicalRepoNameToPath: Map<String, String> = emptyMap(),
)

// TODO: rethink repo mapping handling, is shouldn't be stored inside PersistentStateComponent
@State(
  name = "BazelRepoMappingService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class PersistentBazelRepoMappingService : PersistentStateComponent<BazelRepoMappingServiceState> {
  @Volatile
  var apparentRepoNameToCanonicalName: Map<String, String> = emptyMap()
    set(value) {
      field = value
      canonicalRepoNameToApparentName = value.entries.associate { (apparent, canonical) -> canonical to apparent }
    }

  @Volatile
  var canonicalRepoNameToApparentName: Map<String, String> = emptyMap()

  @Volatile
  var canonicalRepoNameToPath: Map<String, Path> = emptyMap()

  override fun getState(): BazelRepoMappingServiceState? =
    BazelRepoMappingServiceState(
      apparentRepoNameToCanonicalName,
      canonicalRepoNameToPath.mapValues { (_, path) -> path.toString() },
    )

  override fun loadState(state: BazelRepoMappingServiceState) {
    apparentRepoNameToCanonicalName = state.apparentRepoNameToCanonicalName
    canonicalRepoNameToPath = state.canonicalRepoNameToPath.mapValues { (_, path) -> Path(path) }
  }

  companion object {
    fun getInstance(project: Project): PersistentBazelRepoMappingService = project.service<PersistentBazelRepoMappingService>()
  }
}

// keep this service as adapter to have explicit service registration in xml
internal class BazelRepoMappingServiceAdapter(private val project: Project) : BazelRepoMappingService {
  override val apparentRepoNameToCanonicalName: Map<String, String>
    get() = project.service<PersistentBazelRepoMappingService>().apparentRepoNameToCanonicalName
  override val canonicalRepoNameToApparentName: Map<String, String>
    get() = project.service<PersistentBazelRepoMappingService>().canonicalRepoNameToApparentName
  override val canonicalRepoNameToPath: Map<String, Path>
    get() = project.service<PersistentBazelRepoMappingService>().canonicalRepoNameToPath
}
