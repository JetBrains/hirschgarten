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
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.util.awaitCancellationAndInvoke
import com.intellij.util.concurrency.SynchronizedClearableLazy
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path
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
    notifyTargetListUpdated()
  }

  // todo expensive operation
  fun computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo: Map<Label, BuildTarget>): Map<Label, BuildTarget> =
    db.computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo)

  @InternalApi
  fun saveTargets(
    targets: List<RawBuildTarget>,
    fileToTarget: Map<Path, List<Label>>,
    libraryItems: List<LibraryItem>?,
  ) {
    ThreadingAssertions.assertBackgroundThread()
    val labelToTargetInfo = targets.associateByTo(HashMap(targets.size)) { it.id }
    db.reset(
      fileToTarget = fileToTarget,
      fileToExecutableTargets =
        calculateFileToExecutableTargets(
          targetDependentsGraph = TargetDependentsGraph(targets, libraryItems),
          fileToTarget = fileToTarget,
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

  private fun calculateFileToExecutableTargets(
    targetDependentsGraph: TargetDependentsGraph,
    fileToTarget: Map<Path, List<Label>>,
    labelToTargetInfo: Map<Label, BuildTarget>,
  ): Map<Path, List<Label>> {
    val targetToTransitiveRevertedDependenciesCache = mutableMapOf<Label, Set<Label>>()
    return fileToTarget.entries
      .mapNotNull { (path, targets) ->
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
      }.toMap()
  }

  private fun calculateTransitivelyExecutableTargets(
    resultCache: MutableMap<Label, Set<Label>>,
    targetDependentsGraph: TargetDependentsGraph,
    target: Label,
    labelToTargetInfo: Map<Label, BuildTarget>,
  ): Set<Label> =
    resultCache.getOrPut(target) {
      val targetInfo = labelToTargetInfo[target]
      if (targetInfo?.kind?.isExecutable == true) {
        return@getOrPut setOf(target)
      }

      val directDependentIds = targetDependentsGraph.directDependentIds(target)

      val executableTargetsFromSamePackage = directDependentIds.filter {
        it.packagePath == target.packagePath && labelToTargetInfo[it]?.kind?.isExecutable == true
      }
      if (executableTargetsFromSamePackage.isNotEmpty()) return@getOrPut executableTargetsFromSamePackage.toHashSet()

      return@getOrPut directDependentIds
        .asSequence()
        .flatMap { dependency ->
          calculateTransitivelyExecutableTargets(resultCache, targetDependentsGraph, dependency, labelToTargetInfo)
        }.distinct()
        .take(MAX_EXECUTABLE_TARGET_IDS)
        .toHashSet()
    }

  fun notifyTargetListUpdated() {
    emitTargetListUpdate()
    allTargetsAndLibrariesLabelsCache.drop()
    allExecutableTargetsCache.drop()
  }

  internal fun emitTargetListUpdate() {
    check(mutableTargetListUpdated.tryEmit(Unit))
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
