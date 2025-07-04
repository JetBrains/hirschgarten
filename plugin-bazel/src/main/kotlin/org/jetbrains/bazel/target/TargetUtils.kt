@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.util.coroutines.forEachConcurrent
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.util.awaitCancellationAndInvoke
import com.intellij.util.concurrency.SynchronizedClearableLazy
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.experimental.ExperimentalTypeInference
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val MAX_EXECUTABLE_TARGET_IDS = 10

private fun nowAsDuration() = System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS)

private fun getStorageFilename(): String {
  // version for 243 cannot use `Hashing.xxh3_128()`
  // (not available in 243, and we don't want to bundle `hash4j` as a part of Bazel plugin - JIT, increased build script complexity)
  val suffix = if (ApplicationInfo.getInstance().build.baselineVersion <= 243) "-243" else ""
  return "bazel-targets-v2$suffix.db"
}

@PublicApi
@Service(Service.Level.PROJECT)
class TargetUtils(private val project: Project, private val coroutineScope: CoroutineScope) : SettingsSavingComponent {
  private val db = openStore(storeFile = project.getProjectDataPath(getStorageFilename()), filePathSuffix = project.basePath!! + "/")

  // we save only once every 5 minutes, and not earlier than 5 minutes after IDEA startup
  private var lastSaved = nowAsDuration()

  private val allTargetsAndLibrariesLabelsCache =
    SynchronizedClearableLazy {
      db.getAllTargetsAndLibrariesLabelsCache(project)
    }

  @InternalApi
  val allTargetsAndLibrariesLabels: List<String>
    get() = allTargetsAndLibrariesLabelsCache.value

  private val mutableTargetListUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)
  val targetListUpdated: SharedFlow<Unit> = mutableTargetListUpdated.asSharedFlow()

  init {
    // cannot use opt-in (no such opt-in in 243)
    @Suppress("OPT_IN_USAGE")
    coroutineScope.awaitCancellationAndInvoke(Dispatchers.IO) {
      db.close()
    }
  }

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

  fun addFileToTargetIdEntry(file: Path, targets: List<CanonicalLabel>) {
    db.addFileToTarget(file, targets)
  }

  fun removeFileToTargetIdEntry(file: Path) {
    db.removeFileToTarget(file)
  }

  @InternalApi
  @TestOnly
  fun setTargets(labelToTargetInfo: Map<CanonicalLabel, BuildTarget>) {
    db.setTargets(labelToTargetInfo)
    notifyTargetListUpdated()
  }

  // todo expensive operation
  fun computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo: Map<CanonicalLabel, BuildTarget>): Map<CanonicalLabel, BuildTarget> =
    db.computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo)

  @InternalApi
  suspend fun saveTargets(
    targets: List<RawBuildTarget>,
    fileToTarget: Map<Path, List<CanonicalLabel>>,
    libraryItems: List<LibraryItem>?,
  ) {
    val labelToTargetInfo = targets.associateByTo(HashMap(targets.size)) { it.id }
    db.reset(
      fileToTarget = fileToTarget,
      fileToExecutableTargets =
        calculateFileToExecutableTargets(
          libraryItems = libraryItems,
          fileToTarget = fileToTarget,
          targets = targets,
          labelToTargetInfo = labelToTargetInfo,
        ),
      libraryItems = libraryItems,
      targets = targets,
      project = project,
    )

    notifyTargetListUpdated()

    // Explicitly schedule a save since auto-commit is disabled — new data will otherwise remain in memory beyond the configured cache size.
    // This also ensures faster persistence of imported data.
    coroutineScope.launch(Dispatchers.IO + NonCancellable) {
      db.save()
      lastSaved = nowAsDuration()
    }
  }

  private suspend fun calculateFileToExecutableTargets(
    libraryItems: List<LibraryItem>?,
    fileToTarget: Map<Path, List<CanonicalLabel>>,
    targets: List<RawBuildTarget>,
    labelToTargetInfo: Map<CanonicalLabel, BuildTarget>,
  ): Map<Path, List<CanonicalLabel>> =
    withContext(Dispatchers.Default) {
      val targetDependentsGraph = TargetDependentsGraph(targets, libraryItems)
      val targetToTransitiveRevertedDependenciesCache = ConcurrentHashMap<CanonicalLabel, Set<CanonicalLabel>>()
      transformConcurrent(fileToTarget.entries) { (path, targets) ->
        val dependents =
          targets
            .flatMap { label ->
              calculateTransitivelyExecutableTargets(
                resultCache = targetToTransitiveRevertedDependenciesCache,
                targetDependentsGraph = targetDependentsGraph,
                labelToTargetInfo = labelToTargetInfo,
                target = label,
              )
            }.distinct()

        if (dependents.isEmpty()) {
          null
        } else {
          path to dependents
        }
      }
    }

  private fun calculateTransitivelyExecutableTargets(
    resultCache: ConcurrentHashMap<CanonicalLabel, Set<CanonicalLabel>>,
    targetDependentsGraph: TargetDependentsGraph,
    target: CanonicalLabel,
    labelToTargetInfo: Map<CanonicalLabel, BuildTarget>,
  ): Set<CanonicalLabel> =
    resultCache.getOrPut(target) {
      val targetInfo = labelToTargetInfo.get(target)
      if (targetInfo?.kind?.isExecutable == true) {
        return@getOrPut setOf(target)
      }

      val directDependentIds = targetDependentsGraph.directDependentIds(target)
      return@getOrPut directDependentIds
        .asSequence()
        .flatMap { dependency ->
          calculateTransitivelyExecutableTargets(resultCache, targetDependentsGraph, dependency, labelToTargetInfo)
        }.distinct()
        .take(MAX_EXECUTABLE_TARGET_IDS)
        .toHashSet()
    }

  private fun notifyTargetListUpdated() {
    check(mutableTargetListUpdated.tryEmit(Unit))
    allTargetsAndLibrariesLabelsCache.drop()
  }

  @PublicApi
  fun allTargets(): Sequence<CanonicalLabel> = db.getAllTargets()

  fun getTotalTargetCount(): Int = db.getTotalTargetCount()

  @PublicApi
  fun getTargetsForPath(path: Path): List<CanonicalLabel> = db.getTargetsForPath(path) ?: emptyList()

  @PublicApi
  fun getTargetsForFile(file: VirtualFile): List<CanonicalLabel> = file.toNioPathOrNull()?.let { getTargetsForPath(it) } ?: emptyList()

  @InternalApi
  fun getExecutableTargetsForFile(file: VirtualFile): List<CanonicalLabel> {
    val executableDirectTargets =
      getTargetsForFile(file)
        .filter { label -> db.getBuildTargetForLabel(label, project)?.kind?.isExecutable == true }
    if (executableDirectTargets.isEmpty()) {
      return file.toNioPathOrNull()?.let { db.getExecutableTargetsForPath(it) } ?: emptyList()
    }
    return executableDirectTargets
  }

  @PublicApi
  fun isLibrary(target: CanonicalLabel): Boolean = getBuildTargetForLabel(target)?.kind?.ruleType == RuleType.LIBRARY

  @PublicApi
  fun getTargetForModuleId(moduleId: String): CanonicalLabel? = db.getTargetForModuleId(moduleId)

  @PublicApi
  fun getTargetForLibraryId(libraryId: String): CanonicalLabel? = db.getTargetForLibraryId(libraryId)

  /**
   * All labels in a label-to-target map are canonical.
   * The label must be first canonicalized via toCanonicalLabel.
   */
  @InternalApi
  fun getBuildTargetForLabel(label: CanonicalLabel): BuildTarget? = db.getBuildTargetForLabel(label, project)

  @InternalApi
  fun getBuildTargetForModule(module: Module): BuildTarget? = getTargetForModuleId(module.name)?.let { getBuildTargetForLabel(it) }

  @InternalApi
  fun allBuildTargets(): Sequence<BuildTarget> = db.getAllBuildTargets()

  // todo: avoid such methods as we load all targets into memory
  @InternalApi
  fun allBuildTargetAsLabelToTargetMap(predicate: (BuildTarget) -> Boolean): List<CanonicalLabel> =
    db.allBuildTargetAsLabelToTargetMap(predicate)

  fun getTotalFileCount(): Int = db.getTotalFileCount()
}

@InternalApi
fun String.addLibraryModulePrefix() = "_aux.libraries.$this"

@PublicApi
val Project.targetUtils: TargetUtils
  get() = service<TargetUtils>()

@PublicApi
fun CanonicalLabel.getModule(project: Project): Module? = project.targetUtils.getBuildTargetForLabel(this)?.getModule(project)

@PublicApi
fun CanonicalLabel.getModuleEntity(project: Project): ModuleEntity? = getModule(project)?.moduleEntity

val Module.moduleEntity: ModuleEntity?
  @InternalApi
  get() {
    val bridge = this as? ModuleBridge ?: return null
    return bridge.findModuleEntity(bridge.entityStorage.current)
  }

@InternalApi
fun BuildTarget.getModule(project: Project): Module? {
  val moduleName = this.id.formatAsModuleName(project)
  return ModuleManager.getInstance(project).findModuleByName(moduleName)
}

@OptIn(ExperimentalTypeInference::class)
private suspend fun <T : Any, K : Any, V : Any, R : Pair<K, V>> transformConcurrent(
  collection: Collection<T>,
  @BuilderInference action: suspend ProducerScope<R>.(T) -> R?,
): Map<K, V> {
  val flow =
    channelFlow {
      collection.forEachConcurrent {
        val result = action(it)
        if (result != null) {
          channel.send(result)
        }
      }
    }

  val map = HashMap<K, V>()
  flow.collect { value ->
    map.put(value.first, value.second)
  }
  return map
}
