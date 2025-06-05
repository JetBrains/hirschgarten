package org.jetbrains.bazel.languages.starlark.repomapping

import ai.grazie.annotation.TestOnly
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import java.nio.file.Path
import kotlin.io.path.Path

@TestOnly
fun Project.injectCanonicalRepoNameToPath(canonicalRepoNameToPath: Map<String, Path>) {
  val service = BazelRepoMappingService.getInstance(this)
  service.canonicalRepoNameToPath = canonicalRepoNameToPath
}

val Project.apparentRepoNameToCanonicalName: Map<String, String>
  get() =
    BazelRepoMappingService.getInstance(this).apparentRepoNameToCanonicalName.takeIf { it.isNotEmpty() }
      ?: mapOf("" to "")

val Project.canonicalRepoNameToApparentName: Map<String, String>
  get() =
    BazelRepoMappingService.getInstance(this).canonicalRepoNameToApparentName.takeIf { it.isNotEmpty() }
      ?: mapOf("" to "")

val Project.canonicalRepoNameToPath: Map<String, Path>
  get() =
    BazelRepoMappingService.getInstance(this).canonicalRepoNameToPath.takeIf { it.isNotEmpty() }
      ?: mapOf("" to rootDir.toNioPath())

class BazelRepoMappingSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Load bazel repo mapping") {
      val bazelRepoMappingService = BazelRepoMappingService.getInstance(environment.project)
      val bazelRepoMappingResult =
        query("workspace/bazelRepoMapping") {
          environment.server.workspaceBazelRepoMapping()
        }
      bazelRepoMappingService.apparentRepoNameToCanonicalName = bazelRepoMappingResult.apparentRepoNameToCanonicalName
      bazelRepoMappingService.canonicalRepoNameToPath = bazelRepoMappingResult.canonicalRepoNameToPath
    }
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
    internal fun getInstance(project: Project): BazelRepoMappingService = project.service<BazelRepoMappingService>()
  }
}
