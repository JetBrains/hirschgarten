@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.util.AwaitCancellationAndInvoke
import com.intellij.util.awaitCancellationAndInvoke
import com.intellij.util.concurrency.SynchronizedClearableLazy
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.workspaceModel.ide.legacyBridge.findModuleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sync.ExecutableTargetsComputer
import org.jetbrains.bazel.target.TargetsCacheStorage.Companion.openStore
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val MAX_EXECUTABLE_TARGET_IDS = 5

private fun nowAsDuration() = System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS)

/**
 * Increment when making breaking changes to [TargetsCacheStorage]
 */
private const val TARGETS_STORAGE_VERSION: Int = 6

private fun Project.targetsStorageFile(storeVersion: Int): Path = getProjectDataPath("bazel-targets-v$storeVersion.db")

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class TargetUtils(private val project: Project, private val coroutineScope: CoroutineScope) : SettingsSavingComponent {
  @OptIn(AwaitCancellationAndInvoke::class)
  private val dbAsync: Deferred<TargetsCacheStorage> =
    BazelCoroutineService.getInstance(project).startAsync {
      withContext(Dispatchers.IO) {
        (1 until TARGETS_STORAGE_VERSION).forEach { oldVersion -> project.targetsStorageFile(oldVersion).deleteIfExists() }
        val store = openStore(storeFile = project.targetsStorageFile(TARGETS_STORAGE_VERSION), project = project)
        coroutineScope.awaitCancellationAndInvoke(Dispatchers.IO) {
          store.close()
        }
        store
      }
    }

  private val db: TargetsCacheStorage
    get() = runBlocking { dbAsync.await() }

  /**
   * Checks if the storage is initialized and loaded
   */
  fun isLoaded(): Boolean = dbAsync.isCompleted

  /**
   * Executes the given body when the storage is loaded (or immediately if storage is already loaded)
   */
  fun onLoaded(body: () -> Unit) {
    dbAsync.invokeOnCompletion { cause ->
      if (cause == null) {
        body()
      }
    }
  }

  // we save only once every 5 minutes, and not earlier than 5 minutes after IDEA startup
  private var lastSaved = nowAsDuration()

  private val allTargetsShortLabelsCacheCache =
    SynchronizedClearableLazy {
      db.getAllTargets().map { it.toShortString(project) }.toList()
    }

  private val allExecutableTargetsCache = SynchronizedClearableLazy {
    db.getAllBuildTargets()
      .filter { it.kind.isExecutable }
      .map { it.id.toShortString(project) }
      .toList()
  }

  val allTargetShortLabels: List<String>
    get() = allTargetsShortLabelsCacheCache.value

  val allExecutableTargetLabels: List<String>
    get() = allExecutableTargetsCache.value

  private val mutableTargetListUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)
  val targetListUpdated: SharedFlow<Unit> = mutableTargetListUpdated.asSharedFlow()

  override suspend fun save() {
    val exitInProgress = ApplicationManager.getApplication().isExitInProgress
    if (!exitInProgress && (nowAsDuration() - lastSaved) < 5.minutes) {
      return
    }

    withContext(Dispatchers.IO) {
      db.save()
      lastSaved = nowAsDuration()
    }
  }

  fun addFileToTargetIdEntry(file: Path, targets: List<Label>) {
    db.addFileToTarget(file, targets)
  }

  fun removeFileToTargetIdEntry(file: Path) {
    db.removeFileToTarget(file)
  }

  @TestOnly
  fun setTargets(targets: List<BuildTarget>) {
    db.setTargets(targets)
    notifyTargetListUpdated()
  }

  fun saveTargets(
    targets: List<RawBuildTarget>,
    fileToTarget: Map<Path, List<Label>>,
  ) {
    ThreadingAssertions.assertBackgroundThread()

    val executableTargets =
      ExecutableTargetsComputer.calculateExecutableTargets(
        targets = targets,
        labelToTargetInfo = targets.associateByTo(HashMap(targets.size)) { it.id },
      )

    db.reset(
      fileToTarget = fileToTarget,
      executableTargets = executableTargets,
      targets = targets,
    )

    notifyTargetListUpdated()

    // Explicitly schedule a save since auto-commit is disabled — new data will otherwise remain in memory beyond the configured cache size.
    // This also ensures faster persistence of imported data.
    coroutineScope.launch(Dispatchers.IO + NonCancellable) {
      db.save()
      lastSaved = nowAsDuration()
    }
  }

  fun notifyTargetListUpdated() {
    check(mutableTargetListUpdated.tryEmit(Unit))
    allTargetsShortLabelsCacheCache.drop()
    allExecutableTargetsCache.drop()
  }

  fun allTargets(): Sequence<Label> = db.getAllTargets()

  fun getTotalTargetCount(): Int = db.getTotalTargetCount()

  fun getTargetsForPath(path: Path): List<Label> = db.getTargetsForPath(path) ?: emptyList()

  fun getTargetsForFile(file: VirtualFile): List<Label> = file.toNioPathOrNull()?.let { getTargetsForPath(it) } ?: emptyList()

  fun getExecutableTargetsForFile(file: VirtualFile): List<Label> {
    val targetsForFile = getTargetsForFile(file)
    val executableDirectTargets =
      targetsForFile
        .filter { label -> db.getBuildTargetForLabel(label)?.kind?.isExecutable == true }
    if (executableDirectTargets.isEmpty()) {
      return targetsForFile.flatMap { getExecutableTargetsForTarget(it) }.distinct()
    }
    return executableDirectTargets
  }

  fun getExecutableTargetsForTarget(target: Label): List<Label> =
    db.getExecutableTargetsForTarget(target).orEmpty()

  fun isLibrary(target: Label): Boolean = getBuildTargetForLabel(target)?.kind?.ruleType == RuleType.LIBRARY

  /**
   * All labels in a label-to-target map are canonical.
   * The label must be first canonicalized via toCanonicalLabel.
   */
  fun getBuildTargetForLabel(label: Label): BuildTarget? = db.getBuildTargetForLabel(label)

  fun allBuildTargets(): Sequence<BuildTarget> = db.getAllBuildTargets()

  // todo: avoid such methods as we load all targets into memory
  fun allBuildTargetAsLabelToTargetMap(predicate: (BuildTarget) -> Boolean): List<Label> = db.allBuildTargetAsLabelToTargetMap(predicate)

  fun getTotalFileCount(): Int = db.getTotalFileCount()
}

val Project.targetUtils: TargetUtils
  @ApiStatus.Internal
  get() = service<TargetUtils>()

@ApiStatus.Internal
fun Label.getModule(project: Project): Module? = project.targetUtils.getBuildTargetForLabel(this)?.getModule(project)

@ApiStatus.Internal
fun Label.getModuleEntity(project: Project): ModuleEntity? = getModule(project)?.findModuleEntity()

@ApiStatus.Internal
fun BuildTarget.getModule(project: Project): Module? {
  val moduleName = this.id.formatAsModuleName(project)
  return ModuleManager.getInstance(project).findModuleByName(moduleName)
}
