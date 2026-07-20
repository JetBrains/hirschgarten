@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toCanonicalLabelOrThis
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.sync.workspace.persistence.InMemoryWorkspaceTargetMap
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadHint
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndexBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMapBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotMetadata
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraphBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path
import kotlin.reflect.KClass

private val SUMMARY_OPTIONS = TargetLoadOptions(TargetLoadHint.SKIP_FILE_SETS, TargetLoadHint.SKIP_DEPS, TargetLoadHint.SKIP_TARGET_DATA)

private data object EmptyBuildTargetData : BuildTargetData

private fun WorkspaceTarget.toSummary(): TargetSummary =
  TargetSummary(
    key = targetKey,
    kind = rawBuildTarget.kind,
    baseDirectory = rawBuildTarget.baseDirectory,
    isManual = rawBuildTarget.isManual,
    isWorkspace = rawBuildTarget.isWorkspace,
  )

private class CachedSnapshotView(val snapshot: WorkspaceSnapshot, private val project: Project) {
  val importKeys: Set<WorkspaceTargetKey>
  val label2Key: Map<Label, WorkspaceTargetKey>

  init {
    val config = snapshot.syncConfigs.filterIsInstance<CommonWorkspaceSyncConfig>().firstOrNull()
    if (config == null) {
      importKeys = emptySet()
      label2Key = emptyMap()
    }
    else {
      // keep previous behavior, only targets at specific depth (from root targets) are imported
      importKeys = snapshot.targetGraph
        .findAllTargetsAtDepth(maxDepth = config.importDepth, useRelaxedDependencyExpansion = true)
        .map { it.targetKey }
        .toHashSet()

      // precompute label -> key map for compatibility purposes
      val label2Key = HashMap<Label, WorkspaceTargetKey>(importKeys.size)
      for (key in importKeys) {
        // TODO: keep this heuristic until UX features are aware of full target key
        val keyIsPlain = key.aspectIds == WorkspaceAspectIds.EMPTY
        if (label2Key[key.label] == null || keyIsPlain) {
          label2Key[key.label] = key
        }
      }
      this.label2Key = label2Key
    }
  }

  private class TargetSummaryAggregate(val summaries: List<TargetSummary>, val byKey: Map<WorkspaceTargetKey, TargetSummary>)

  private val aggregate: TargetSummaryAggregate by lazy {
    // using cursor here is way faster than random per-key lookups
    val importedSummaryTargets = snapshot.targets.allTargets(SUMMARY_OPTIONS)
      .filter { it.targetKey in importKeys }
      .map { it.toSummary() }
      .toList()
    TargetSummaryAggregate(
      summaries = importedSummaryTargets,
      byKey = importedSummaryTargets.associateBy { it.key },
    )
  }

  val summaries: List<TargetSummary>
    get() = aggregate.summaries

  val label2Summary: Map<Label, TargetSummary> by lazy {
    label2Key.mapNotNull { (label, key) -> aggregate.byKey[key]?.let { label to it } }.toMap()
  }

  fun warmUp() {
    aggregate
    label2Summary
  }

  val shortLabels: List<String> by lazy {
    summaries.map { it.id.toShortString(project) }
  }

  val executableShortLabels: List<String> by lazy {
    summaries.filter { it.kind.isExecutable }
      .map { it.id.toShortString(project) }
  }

  fun targetsForPath(path: Path): List<Label> =
    snapshot.fileToTarget.getTargetsByFile(path)
      .filter { it in importKeys }
      .map { it.label }

  fun executableTargetsFor(label: Label): List<Label> =
    label.toCanonicalLabelOrThis(project)?.let { snapshot.executableTargets.executableTargetsFor(it) }.orEmpty()

  // small data cache to accelerate repeated bulk reads of the same target data
  private val dataCache = Caffeine.newBuilder()
    .maximumSize(64)
    .build<Pair<Label, KClass<*>>, BuildTargetData>()

  fun <T : BuildTargetData> data(label: Label, type: KClass<T>): T? {
    val cached = dataCache.get(label to type) {
      val key = label2Key[label] ?: return@get EmptyBuildTargetData
      val options = TargetLoadOptions(
        hints = setOf(TargetLoadHint.SKIP_FILE_SETS, TargetLoadHint.SKIP_DEPS, TargetLoadHint.SKIP_TARGET_DATA),
        dataTypes = setOf(type),
      )
      snapshot.targets.findTargetByKey(key, options)?.rawBuildTarget?.data
        ?.firstOrNull { type.isInstance(it) } ?: EmptyBuildTargetData
    }
    return if (cached === EmptyBuildTargetData) {
      null
    }
    else {
      type.java.cast(cached)
    }
  }
}

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class TargetStorage(private val project: Project, private val coroutineScope: CoroutineScope) {
  private val service: WorkspaceSnapshotService
    get() = project.service<WorkspaceSnapshotService>()

  private val initialLoad: Job = coroutineScope.launch { service.currentSnapshot() }

  private val mutableTargetListUpdated = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val targetListUpdated: SharedFlow<Unit> = mutableTargetListUpdated.asSharedFlow()
  private val views = MutableStateFlow(CachedSnapshotView(WorkspaceSnapshot.EMPTY, project))

  init {
    coroutineScope.launch {
      service.snapshot.collect { snapshot ->
        if (service.snapshot.value === snapshot) {
          viewFor(snapshot).warmUp()
        }
        mutableTargetListUpdated.tryEmit(Unit)
      }
    }
  }

  fun isLoaded(): Boolean = initialLoad.isCompleted

  suspend fun awaitLoaded() {
    initialLoad.join()
  }

  fun onLoaded(body: () -> Unit) {
    coroutineScope.launch {
      awaitLoaded()
      body()
    }
  }

  private fun currentSnapshot(): WorkspaceSnapshot {
    if (!initialLoad.isCompleted) {
      runBlocking { initialLoad.join() }
    }
    return service.snapshot.value
  }

  private fun view(): CachedSnapshotView = viewFor(currentSnapshot())

  private fun viewFor(snapshot: WorkspaceSnapshot): CachedSnapshotView =
    views.updateAndGet { current -> if (current.snapshot === snapshot) current else CachedSnapshotView(snapshot, project) }

  val allTargetShortLabels: List<String>
    get() = view().shortLabels

  val allExecutableTargetLabels: List<String>
    get() = view().executableShortLabels

  fun addFileToTargetIdEntry(file: Path, targets: Collection<Label>) {
    val view = view()
    view.snapshot.fileToTarget.addMapping(file, targets.mapNotNull { view.label2Key[it] })
  }

  fun removeFileToTargetIdEntry(file: Path) {
    view().snapshot.fileToTarget.removeMapping(file)
  }

  @TestOnly
  fun setTargets(targets: List<BuildTarget>) {
    val workspaceTargets = targets.map { target ->
      val key = WorkspaceTargetKey(label = target.id)
      WorkspaceTarget(
        targetKey = key,
        rawBuildTarget = RawBuildTarget(
          key = key,
          dependencies = emptyList(),
          kind = target.kind,
          sources = SourceFileCollection.EMPTY,
          generatedSources = SourceFileCollection.EMPTY,
          resources = SourceFileCollection.EMPTY,
          baseDirectory = target.baseDirectory,
          data = target.data,
          isManual = target.isManual,
          isWorkspace = target.isWorkspace,
        ),
      )
    }
    val graph = WorkspaceTargetGraphBuilder.build(
      rootTargets = workspaceTargets.map { it.targetKey }.toSet(),
      targets = workspaceTargets,
    )
    val snapshot = WorkspaceSnapshot(
      targets = InMemoryWorkspaceTargetMap(workspaceTargets.associateBy { it.targetKey }),
      configurations = mapOf(),
      targetGraph = graph,
      fileToTarget = File2TargetMapBuilder.build(targets = workspaceTargets),
      executableTargets = ExecutableTargetsIndexBuilder.build(targetGraph = graph, importDepth = 0, targets = workspaceTargets),
      syncConfigs = listOf(
        CommonWorkspaceSyncConfig(
          projectRootDir = Path.of(project.basePath!!),
          projectName = project.name,
          importDepth = 0,
        ),
      ),
      repoMapping = RepoMappingDisabled,
      metadata = WorkspaceSnapshotMetadata(version = 1),
    )
    runBlocking { service.update { snapshot } }
  }

  fun allTargets(): Sequence<Label> = allTargetSummaries().asSequence().map { it.id }

  fun allTargetSummaries(): List<TargetSummary> = view().summaries

  fun getTargetSummary(label: Label): TargetSummary? =
    label.toCanonicalLabelOrThis(project)?.let { view().label2Summary[it] }

  fun getTotalTargetCount(): Int = view().summaries.size

  fun getTargetsForPath(path: Path): List<Label> = view().targetsForPath(path)

  fun getTargetsForFile(file: VirtualFile): List<Label> = file.toNioPathOrNull()?.let { getTargetsForPath(it) } ?: emptyList()

  fun getExecutableTargetsForFile(file: VirtualFile): List<Label> {
    val path = file.toNioPathOrNull() ?: return emptyList()
    // one view for the whole answer, otherwise the file lookup and the summaries can come from different snapshots
    val view = view()
    val targetsForFile = view.targetsForPath(path)
    val executableDirectTargets = targetsForFile.filter { label -> view.label2Summary[label]?.kind?.isExecutable == true }
    if (executableDirectTargets.isEmpty()) {
      return targetsForFile.flatMap { view.executableTargetsFor(it) }.distinct()
    }
    return executableDirectTargets
  }

  fun getExecutableTargetsForTarget(target: Label): List<Label> = view().executableTargetsFor(target)

  fun isLibrary(target: Label): Boolean = getTargetSummary(target)?.kind?.ruleType == RuleType.LIBRARY

  fun <T : BuildTargetData> getTargetDataForLabel(label: Label, type: KClass<T>): T? =
    label.toCanonicalLabelOrThis(project)?.let { view().data(it, type) }

  fun getTotalFileCount(): Int = view().snapshot.fileToTarget.size
}

@ApiStatus.Internal
inline fun <reified T : BuildTargetData> TargetStorage.getTargetDataForLabel(label: Label): T? = getTargetDataForLabel(label, T::class)

val Project.targetStorage: TargetStorage
  @ApiStatus.Internal
  get() = service<TargetStorage>()
