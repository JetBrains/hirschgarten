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
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
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
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.target.TargetsCacheStorage.Companion.openStore
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val MAX_EXECUTABLE_TARGET_IDS = 10

private fun nowAsDuration() = System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS)

@PublicApi
@Service(Service.Level.PROJECT)
class TargetUtils(private val project: Project, private val coroutineScope: CoroutineScope) : SettingsSavingComponent {
  @OptIn(AwaitCancellationAndInvoke::class)
  private val dbAsync: Deferred<TargetsCacheStorage> =
    BazelCoroutineService.getInstance(project).startAsync {
      withContext(Dispatchers.IO) {
        val store = openStore(storeFile = project.getProjectDataPath("bazel-targets-v3.db"), project = project)
        coroutineScope.awaitCancellationAndInvoke(Dispatchers.IO) {
          store.close()
        }
        store
      }
    }

  private val db: TargetsCacheStorage
    get() = runBlocking { dbAsync.await() }

  // we save only once every 5 minutes, and not earlier than 5 minutes after IDEA startup
  private var lastSaved = nowAsDuration()

  private val allTargetsAndLibrariesLabelsCache =
    SynchronizedClearableLazy {
      db.getAllTargetsAndLibrariesLabelsCache()
    }

  private val allExecutableTargetsCache = SynchronizedClearableLazy {
    db.getAllBuildTargets()
      .filter { it.kind.isExecutable }
      .map { it.id.toShortString(project) }
      .toList()
  }

  @InternalApi
  val allTargetsAndLibrariesLabels: List<String>
    get() = allTargetsAndLibrariesLabelsCache.value

  @InternalApi
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

  @InternalApi
  @TestOnly
  fun setTargets(targets: List<BuildTarget>) {
    db.setTargets(targets)
    notifyTargetListUpdated()
  }

  // todo expensive operation
  fun computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo: Map<Label, BuildTarget>): Map<Label, BuildTarget> =
    db.computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo)

  @InternalApi
  fun saveTargets(
    targets: List<RawBuildTarget>,
    fileToTarget: Map<Path, List<Label>>,
    libraryItems: List<LibraryItem>,
  ) {
    ThreadingAssertions.assertBackgroundThread()

    val executableTargets =
      calculateExecutableTargets(
        targets = fileToTarget.flatMap { it.value }.distinct(),
        targetDirectDependentsGraph = calculateDirectDependentsGraph(targets),
        labelToTargetInfo = targets.associateByTo(HashMap(targets.size)) { it.id },
      )

    db.reset(
      fileToTarget = fileToTarget,
      executableTargets = executableTargets,
      libraryItems = libraryItems,
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

  private fun calculateDirectDependentsGraph(targets: List<RawBuildTarget>): Map<Label, Set<Label>> {
    val targetIdToDirectDependentIds = hashMapOf<Label, MutableSet<Label>>()
    for (targetInfo in targets) {
      val dependencies = targetInfo.dependencies
      for (dependency in dependencies) {
        targetIdToDirectDependentIds
          .computeIfAbsent(dependency.label) { hashSetOf<Label>() }
          .add(targetInfo.id)
      }
    }
    return targetIdToDirectDependentIds
  }

  private fun calculateExecutableTargets(
    targets: List<Label>,
    targetDirectDependentsGraph: Map<Label, Set<Label>>,
    labelToTargetInfo: Map<Label, RawBuildTarget>,
  ): Map<ResolvedLabel, List<Label>> {
    val targetToTransitiveRevertedDependenciesCache = mutableMapOf<Label, Set<Label>>()
    val result = mutableMapOf<ResolvedLabel, MutableList<Label>>()
    targets
      .forEach { label ->
        val executables = calculateTransitivelyExecutableTargets(
          resultCache = targetToTransitiveRevertedDependenciesCache,
          targetDirectDependentsGraph = targetDirectDependentsGraph,
          labelToTargetInfo = labelToTargetInfo,
          target = label,
        )
        if (executables.isNotEmpty()) {
          result[label as ResolvedLabel] = executables.toMutableList()
        }
      }
    labelToTargetInfo.forEach { (label, target) ->
      target.generatorName?.let { generatorName ->
        val generatorLabel = label.assumeResolved().copy(target = SingleTarget(generatorName))
        val generatorTargets = result.getOrPut(generatorLabel) { mutableListOf() }
        if (generatorTargets.size < MAX_EXECUTABLE_TARGET_IDS) {
          generatorTargets.add(label)
        }
      }
    }
    return result
  }

  private fun calculateTransitivelyExecutableTargets(
    resultCache: MutableMap<Label, Set<Label>>,
    targetDirectDependentsGraph: Map<Label, Set<Label>>,
    target: Label,
    labelToTargetInfo: Map<Label, BuildTarget>,
  ): Set<Label> =
    resultCache.getOrPut(target) {
      val targetInfo = labelToTargetInfo[target]
      if (targetInfo?.kind?.isExecutable == true) {
        return@getOrPut setOf(target)
      }

      val directDependentIds = targetDirectDependentsGraph[target] ?: return@getOrPut emptySet()

      val executableTargetsFromSamePackage = directDependentIds.filter {
        it.packagePath == target.packagePath && labelToTargetInfo[it]?.kind?.isExecutable == true
      }
      if (executableTargetsFromSamePackage.isNotEmpty()) return@getOrPut executableTargetsFromSamePackage.toHashSet()

      return@getOrPut directDependentIds
        .asSequence()
        .flatMap { dependency ->
          calculateTransitivelyExecutableTargets(resultCache, targetDirectDependentsGraph, dependency, labelToTargetInfo)
        }.distinct()
        .take(MAX_EXECUTABLE_TARGET_IDS)
        .toHashSet()
    }

  private fun notifyTargetListUpdated() {
    check(mutableTargetListUpdated.tryEmit(Unit))
    allTargetsAndLibrariesLabelsCache.drop()
    allExecutableTargetsCache.drop()
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
  fun getBuildTargetForLabel(label: Label): BuildTarget? = db.getBuildTargetForLabel(label)

  @InternalApi
  fun getBuildTargetForModule(module: Module): BuildTarget? = getTargetForModuleId(module.name)?.let { getBuildTargetForLabel(it) }

  @InternalApi
  fun allBuildTargets(): Sequence<BuildTarget> = db.getAllBuildTargets()

  // todo: avoid such methods as we load all targets into memory
  @InternalApi
  fun allBuildTargetAsLabelToTargetMap(predicate: (BuildTarget) -> Boolean): List<Label> = db.allBuildTargetAsLabelToTargetMap(predicate)

  fun getTotalFileCount(): Int = db.getTotalFileCount()
}

private val LIBRARY_MODULE_PREFIX = "_aux.libraries."

@InternalApi
fun String.addLibraryModulePrefix() = LIBRARY_MODULE_PREFIX + this

@InternalApi
fun String.isLibraryModule() = startsWith(LIBRARY_MODULE_PREFIX)

@PublicApi
val Project.targetUtils: TargetUtils
  get() = service<TargetUtils>()

@PublicApi
fun Label.getModule(project: Project): Module? = project.targetUtils.getBuildTargetForLabel(this)?.getModule(project)

@PublicApi
fun Label.getModuleEntity(project: Project): ModuleEntity? = getModule(project)?.moduleEntity

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
