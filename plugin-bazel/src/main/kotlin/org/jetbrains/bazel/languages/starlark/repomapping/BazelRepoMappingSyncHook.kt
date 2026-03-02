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
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.withSubtask
import java.nio.file.Path
import kotlin.io.path.Path

@TestOnly
@ApiStatus.Internal
fun Project.injectCanonicalRepoNameToPath(canonicalRepoNameToPath: Map<String, Path>) {
  val service = BazelRepoMappingService.getInstance(this)
  service.canonicalRepoNameToPath = canonicalRepoNameToPath
}

@TestOnly
@ApiStatus.Internal
fun Project.injectCanonicalRepoNameToApparentName(canonicalRepoNameToApparentName: Map<String, String>) {
  val service = BazelRepoMappingService.getInstance(this)
  service.canonicalRepoNameToApparentName = canonicalRepoNameToApparentName
}

internal val Project.apparentRepoNameToCanonicalName: Map<String, String>
  get() =
    BazelRepoMappingService.getInstance(this).apparentRepoNameToCanonicalName.takeIf { it.isNotEmpty() }
      ?: mapOf("" to "")

internal val Project.canonicalRepoNameToApparentName: Map<String, String>
  get() =
    BazelRepoMappingService.getInstance(this).canonicalRepoNameToApparentName.takeIf { it.isNotEmpty() }
      ?: mapOf("" to "")

internal val Project.canonicalRepoNameToPath: Map<String, Path>
  get() =
    BazelRepoMappingService.getInstance(this).canonicalRepoNameToPath.takeIf { it.isNotEmpty() }
      ?: mapOf("" to rootDir.toNioPath())

internal class BazelRepoMappingSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Load bazel repo mapping") {
      val bazelRepoMappingService = BazelRepoMappingService.getInstance(environment.project)
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

@State(
  name = "BazelRepoMappingService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class BazelRepoMappingService : PersistentStateComponent<BazelRepoMappingServiceState> {
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
    fun getInstance(project: Project): BazelRepoMappingService = project.service<BazelRepoMappingService>()
  }
}
