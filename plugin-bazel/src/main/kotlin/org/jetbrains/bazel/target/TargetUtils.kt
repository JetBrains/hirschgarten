@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
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
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.LibraryGraph
import org.jetbrains.bazel.target.TargetsCacheStorage.Companion.openStore
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path
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

private val LOG = logger<TargetUtils>()

@PublicApi
@Service(Service.Level.PROJECT)
@ApiStatus.Internal
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
  // ...existing code...

  private fun logThreadPoolDiagnostics(phase: String) {
    try {

      // Use Java ThreadMXBean to get thread statistics
      val threadMXBean = java.lang.management.ManagementFactory.getThreadMXBean()
      val allThreads = threadMXBean.allThreadIds
      val threadInfos = threadMXBean.getThreadInfo(allThreads, 0)

      // Count threads by state and name pattern
      var ioThreadsRunning = 0
      var ioThreadsWaiting = 0
      var ioThreadsBlocked = 0
      var totalIOThreads = 0

      for (info in threadInfos) {
        if (info != null && info.threadName.contains("DefaultDispatcher-worker", ignoreCase = true)) {
          totalIOThreads++
          when (info.threadState) {
            Thread.State.RUNNABLE -> ioThreadsRunning++
            Thread.State.WAITING, Thread.State.TIMED_WAITING -> ioThreadsWaiting++
            Thread.State.BLOCKED -> ioThreadsBlocked++
            else -> {}
          }
        }
      }

      LOG.info(
        "Thread pool diagnostics ($phase): " +
        "IO threads total=$totalIOThreads, running=$ioThreadsRunning, waiting=$ioThreadsWaiting, blocked=$ioThreadsBlocked, " +
        "current thread=${Thread.currentThread().name}"
      )

      // Also log total system thread count for context
      LOG.info("System total threads: ${threadMXBean.threadCount}, daemon threads: ${threadMXBean.daemonThreadCount}")

    } catch (e: Exception) {
      LOG.warn("Failed to collect thread pool diagnostics", e)
    }
  }

  // we save only once every 5 minutes, and not earlier than 5 minutes after IDEA startup
  private var lastSaved = nowAsDuration()

  // Throttle saves to avoid freezing - save at most once per minute
  private val saveThrottleMillis = 60_000L
  private var pendingSave: kotlinx.coroutines.Job? = null

  // Dedicated single-thread dispatcher for non-blocking commits
  // This ensures commits happen sequentially and don't block the IO pool
  private val commitDispatcher = Dispatchers.IO.limitedParallelism(1)

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

  val allTargetsAndLibrariesLabels: List<String>
    get() = allTargetsAndLibrariesLabelsCache.value

  val allExecutableTargetLabels: List<String>
    get() = allExecutableTargetsCache.value

  private val mutableTargetListUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)
  val targetListUpdated: SharedFlow<Unit> = mutableTargetListUpdated.asSharedFlow()

  override suspend fun save() {
    // Throttle saves to prevent excessive disk I/O
    val now = nowAsDuration()
    if ((now - lastSaved).inWholeMilliseconds < saveThrottleMillis) {
      return
    }
    db.save()
    lastSaved = now
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

  fun addTargets(labelToTargetInfo: Map<Label, BuildTarget>, project: Project) {
    db.addTargets(labelToTargetInfo, project)
    notifyTargetListUpdated()
  }

  // todo expensive operation
  fun computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo: Map<Label, BuildTarget>): Map<Label, BuildTarget> =
    db.computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo)

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

    // Fire-and-forget async save on dedicated single-thread dispatcher
    // This ensures commits don't block the shared IO pool or reads
    // The commitDispatcher has parallelism=1 so saves happen sequentially
    pendingSave?.cancel()
    pendingSave = coroutineScope.launch(commitDispatcher + NonCancellable) {
      val now = nowAsDuration()
      if ((now - lastSaved).inWholeMilliseconds >= saveThrottleMillis) {
        val startTime = System.currentTimeMillis()

        // Diagnostic: Log thread pool info before save
        logThreadPoolDiagnostics("before save")

        try {
          // This blocks only the dedicated commit thread, not the main IO pool
          // Reads can continue during the commit using in-memory cached data
          db.save()
          lastSaved = now

          val duration = System.currentTimeMillis() - startTime

          // Diagnostic: Log thread pool info after save
          logThreadPoolDiagnostics("after save")

          // Log performance metrics
          LOG.info("TargetUtils database save completed in ${duration}ms (targets: ${targets.size}, files: ${fileToTarget.size})")

          if (duration > 1000) {
            LOG.warn("TargetUtils database save took ${duration}ms - this may indicate I/O bottleneck")
          }
        } catch (e: Exception) {
          LOG.error("Failed to save TargetUtils database", e)
        }
      }
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
    visited: MutableSet<Label> = HashSet(),
  ): Set<Label> =
    resultCache.getOrPut(target) {
      // Check if we've already visited this target in the current path to prevent cycles
      if (target in visited) {
        return@getOrPut emptySet()
      }

      val targetInfo = labelToTargetInfo[target]
      if (targetInfo?.kind?.isExecutable == true) {
        return@getOrPut setOf(target)
      }

      // Add current target to visited set
      visited.add(target)

      val directDependentIds = targetDirectDependentsGraph[target] ?: return@getOrPut emptySet()

      val executableTargetsFromSamePackage = directDependentIds.filter {
        it.packagePath == target.packagePath && labelToTargetInfo[it]?.kind?.isExecutable == true
      }
      if (executableTargetsFromSamePackage.isNotEmpty()) {
        val result = executableTargetsFromSamePackage.toHashSet()
        visited.remove(target)
        return@getOrPut result
      }

      val result = directDependentIds
        .asSequence()
        .flatMap { dependency ->
          calculateTransitivelyExecutableTargets(resultCache, targetDirectDependentsGraph, dependency, labelToTargetInfo, visited)
        }.distinct()
        .take(MAX_EXECUTABLE_TARGET_IDS)
        .toHashSet()
      // Remove current target from visited set (backtracking)
      visited.remove(target)
      return@getOrPut result
    }

  fun notifyTargetListUpdated() {
    check(mutableTargetListUpdated.tryEmit(Unit))
    allTargetsAndLibrariesLabelsCache.drop()
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

  fun getTargetForModuleId(moduleId: String): Label? = db.getTargetForModuleId(moduleId)

  fun getTargetForLibraryId(libraryId: String): Label? = db.getTargetForLibraryId(libraryId)

  /**
   * All labels in a label-to-target map are canonical.
   * The label must be first canonicalized via toCanonicalLabel.
   */
  fun getBuildTargetForLabel(label: Label): BuildTarget? = db.getBuildTargetForLabel(label)

  fun getBuildTargetForModule(module: Module): BuildTarget? = getTargetForModuleId(module.name)?.let { getBuildTargetForLabel(it) }

  fun allBuildTargets(): Sequence<BuildTarget> = db.getAllBuildTargets()

  // todo: avoid such methods as we load all targets into memory
  fun allBuildTargetAsLabelToTargetMap(predicate: (BuildTarget) -> Boolean): List<Label> = db.allBuildTargetAsLabelToTargetMap(predicate)

  fun getTotalFileCount(): Int = db.getTotalFileCount()
}

internal fun String.isLibraryModule() = startsWith(LibraryGraph.LIBRARY_MODULE_PREFIX)

val Project.targetUtils: TargetUtils
  @ApiStatus.Internal
  get() = service<TargetUtils>()

internal fun Label.getModule(project: Project): Module? = project.targetUtils.getBuildTargetForLabel(this)?.getModule(project)

internal fun Label.getModuleEntity(project: Project): ModuleEntity? = getModule(project)?.moduleEntity

internal val Module.moduleEntity: ModuleEntity?
  get() {
    val bridge = this as? ModuleBridge ?: return null
    return bridge.findModuleEntity(bridge.entityStorage.current)
  }

internal fun BuildTarget.getModule(project: Project): Module? {
  val moduleName = this.id.formatAsModuleName(project)
  return ModuleManager.getInstance(project).findModuleByName(moduleName)
}
