package org.jetbrains.bazel.languages.starlark.repomapping

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.SynchronizedClearableLazy
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.sync_new.BazelSyncV2
import org.jetbrains.bazel.sync_new.flow.BzlmodSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.DisabledSyncRepoMapping
import org.jetbrains.bazel.sync_new.flow.universe.syncRepoMapping
import java.nio.file.Path
import kotlin.io.path.Path

@TestOnly
fun Project.injectCanonicalRepoNameToPath(canonicalRepoNameToPath: Map<String, Path>) {
  val service = BazelRepoMappingService.getInstance(this)
  service.canonicalRepoNameToPath = canonicalRepoNameToPath
}

@TestOnly
fun Project.injectCanonicalRepoNameToApparentName(canonicalRepoNameToApparentName: Map<String, String>) {
  val service = BazelRepoMappingService.getInstance(this)
  service.canonicalRepoNameToApparentName = canonicalRepoNameToApparentName
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
class BazelRepoMappingService(
  private val project: Project,
) : PersistentStateComponent<BazelRepoMappingServiceState> {

  private val apparentRepoNameToCanonicalNameV2Cached = SynchronizedClearableLazy {
    when (val mapping = project.syncRepoMapping) {
      is BzlmodSyncRepoMapping -> mapping.apparentToCanonical
      DisabledSyncRepoMapping -> emptyMap()
    }
  }

  private val canonicalRepoNameToPathV2Cached = SynchronizedClearableLazy {
    when (val mapping = project.syncRepoMapping) {
      is BzlmodSyncRepoMapping -> mapping.apparentToCanonical.entries.associate { (canonical, apparent) -> apparent to canonical }
      DisabledSyncRepoMapping -> emptyMap()
    }
  }

  @Volatile
  var apparentRepoNameToCanonicalName: Map<String, String> = emptyMap()
    get() {
      return if (BazelSyncV2.isEnabled) {
        apparentRepoNameToCanonicalNameV2Cached.get()
      } else {
        field
      }
    }
    set(value) {
      field = value
      canonicalRepoNameToApparentName = value.entries.associate { (apparent, canonical) -> canonical to apparent }
    }

  @Volatile
  var canonicalRepoNameToApparentName: Map<String, String> = emptyMap()
    get() {
      return if (BazelSyncV2.isEnabled) {
        canonicalRepoNameToPathV2Cached.get()
      } else {
        field
      }
    }

  @Volatile
  var canonicalRepoNameToPath: Map<String, Path> = emptyMap()
    get() {
      return if (BazelSyncV2.isEnabled) {
        when (val mapping = project.syncRepoMapping) {
          is BzlmodSyncRepoMapping -> mapping.canonicalRepoNameToLocalPath
          DisabledSyncRepoMapping -> emptyMap()
        }
      } else {
        field
      }
    }

  override fun getState(): BazelRepoMappingServiceState? =
    BazelRepoMappingServiceState(
      apparentRepoNameToCanonicalName,
      canonicalRepoNameToPath.mapValues { (_, path) -> path.toString() },
    )

  override fun loadState(state: BazelRepoMappingServiceState) {
    apparentRepoNameToCanonicalName = state.apparentRepoNameToCanonicalName
    canonicalRepoNameToPath = state.canonicalRepoNameToPath.mapValues { (_, path) -> Path(path) }
  }

  fun dropCaches() {
    canonicalRepoNameToPathV2Cached.drop()
    apparentRepoNameToCanonicalNameV2Cached.drop()
  }

  companion object {
    fun getInstance(project: Project): BazelRepoMappingService = project.service<BazelRepoMappingService>()
  }
}
