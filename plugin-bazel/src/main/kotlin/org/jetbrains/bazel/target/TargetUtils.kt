@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
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
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
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
  // (not available in 243 and we don't want to bundle `hash4j` as a part of Bazel plugin - JIT, increased build script complexity)
  return "bazel-targets-v${if (ApplicationInfo.getInstance().build.baselineVersion <= 243) "v0.5" else "1"}.db"
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

  // todo: remove (used only during sync)
  var fileToTargetWithoutLowPrioritySharedSources: Map<Path, List<Label>> = emptyMap()
    private set

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

  fun addFileToTargetIdEntry(file: Path, targets: List<Label>) {
    db.addFileToTarget(file, targets)
  }

  fun removeFileToTargetIdEntry(file: Path) {
    db.removeFileToTarget(file)
  }

  @InternalApi
  @TestOnly
  fun setTargets(labelToTargetInfo: Map<Label, BuildTarget>) {
    db.setTargets(labelToTargetInfo)
    updateComputedFields()
  }

  // todo expensive operation
  fun computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo: Map<Label, BuildTarget>): Map<Label, BuildTarget> =
    db.computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo)

  @InternalApi
  fun getSharedFiles(): Map<Path, List<Label>> = fileToTargetWithoutLowPrioritySharedSources

  @InternalApi
  suspend fun saveTargets(
    targets: List<BuildTarget>,
    fileToTarget: Map<Path, List<Label>>,
    fileToTargetWithoutLowPrioritySharedSources: Map<Path, List<Label>>,
    libraryItems: List<LibraryItem>?,
  ) {
    this.fileToTargetWithoutLowPrioritySharedSources = fileToTargetWithoutLowPrioritySharedSources

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

    // Explicitly schedule a save since auto-commit is disabled — new data will otherwise remain in memory beyond the configured cache size.
    // This also ensures faster persistence of imported data.
    coroutineScope.launch(Dispatchers.IO + NonCancellable) {
      db.save()
      lastSaved = nowAsDuration()
    }
    updateComputedFields()
  }

  private suspend fun calculateFileToExecutableTargets(
    libraryItems: List<LibraryItem>?,
    fileToTarget: Map<Path, List<Label>>,
    targets: List<BuildTarget>,
    labelToTargetInfo: Map<Label, BuildTarget>,
  ): Map<Path, List<Label>> =
    withContext(Dispatchers.Default) {
      val targetDependentsGraph = TargetDependentsGraph(targets, libraryItems)
      val targetToTransitiveRevertedDependenciesCache = ConcurrentHashMap<Label, Set<Label>>()
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
    resultCache: ConcurrentHashMap<Label, Set<Label>>,
    targetDependentsGraph: TargetDependentsGraph,
    target: Label,
    labelToTargetInfo: Map<Label, BuildTarget>,
  ): Set<Label> =
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

  private fun updateComputedFields() {
    allTargetsAndLibrariesLabelsCache.drop()
  }

  @PublicApi
  fun allTargets(): Sequence<Label> = db.getAllTargets()

  fun getTotalTargetCount(): Int = db.getTotalTargetCount()

  @PublicApi
  fun getTargetsForPath(path: Path): List<Label> = db.getTargetsForPath(path) ?: emptyList()

  @PublicApi
  fun getTargetsForFile(file: VirtualFile): List<Label> = file.toNioPathOrNull()?.let { getTargetsForPath(it) } ?: emptyList()

  @InternalApi
  fun getExecutableTargetsForFile(file: VirtualFile): List<Label> {
    val executableDirectTargets =
      getTargetsForFile(file)
        .filter { label -> db.getBuildTargetForLabel(label, project)?.kind?.isExecutable == true }
    if (executableDirectTargets.isEmpty()) {
      return file.toNioPathOrNull()?.let { db.getExecutableTargetsForPath(it) } ?: emptyList()
    }
    return executableDirectTargets
  }

  @PublicApi
  fun isLibrary(target: Label): Boolean = getBuildTargetForLabel(target)?.kind?.ruleType == RuleType.LIBRARY

  @PublicApi
  fun getTargetForModuleId(moduleId: String): Label? = db.getTargetForModuleId(moduleId)

  @PublicApi
  fun getTargetForLibraryId(libraryId: String): Label? = db.getTargetForLibraryId(libraryId)

  /**
   * All labels in a label-to-target map are canonical.
   * The label must be first canonicalized via toCanonicalLabel.
   */
  @InternalApi
  fun getBuildTargetForLabel(label: Label): BuildTarget? = db.getBuildTargetForLabel(label, project)

  @InternalApi
  fun getBuildTargetForModule(module: com.intellij.openapi.module.Module): BuildTarget? =
    getTargetForModuleId(module.name)?.let { getBuildTargetForLabel(it) }

  @InternalApi
  fun allBuildTargets(): Sequence<BuildTarget> = db.getAllBuildTargets()

  // todo: avoid such methods as we load all targets into memory
  @InternalApi
  fun allBuildTargetAsLabelToTargetMap(): Map<Label, BuildTarget> = db.allBuildTargetAsLabelToTargetMap()

  fun getTotalFileCount(): Int = db.getTotalFileCount()
}

@InternalApi
fun String.addLibraryModulePrefix() = "_aux.libraries.$this"

@PublicApi
val Project.targetUtils: TargetUtils
  get() = service<TargetUtils>()

@PublicApi
fun Label.getModule(project: Project): com.intellij.openapi.module.Module? =
  project.targetUtils.getBuildTargetForLabel(this)?.getModule(project)

@PublicApi
fun Label.getModuleEntity(project: Project): ModuleEntity? = getModule(project)?.moduleEntity

val com.intellij.openapi.module.Module.moduleEntity: ModuleEntity?
  @InternalApi
  get() {
    val bridge = this as? ModuleBridge ?: return null
    return bridge.findModuleEntity(bridge.entityStorage.current)
  }

@InternalApi
fun BuildTarget.getModule(project: Project): com.intellij.openapi.module.Module? {
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
