@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.util.coroutines.flow.mapStateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toCanonicalLabelOrThis
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver
import org.jetbrains.bazel.sync.workspace.persistence.BuildTargetLoadHint
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.TargetSection
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationResolver
import org.jetbrains.bsp.protocol.data
import org.jetbrains.bsp.protocol.id
import org.jetbrains.bsp.protocol.isManual
import java.nio.file.Path

private data object EmptyBuildTargetData : BuildTargetData

@ApiStatus.Internal
class CachedSnapshotView(val snapshot: WorkspaceSnapshot) {
  val importKeys: Set<WorkspaceTargetKey>
  val label2Key: Map<Label, WorkspaceTargetKey>
  val outputLocationResolver: OutputLocationResolver

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
    outputLocationResolver = DefaultOutputLocationResolver.createExecrootResolving(snapshot.bazelInfo)
  }

  private val target2Summary: Map<WorkspaceTargetKey, BuildTarget> =
    snapshot.targets.allTargets(TargetLoadOptions.SUMMARY)
      .filter { it.key in importKeys }
      .associateBy { it.key }

  val summaries: List<BuildTarget> = target2Summary.values.toList()

  val label2Summary: Map<Label, BuildTarget> =
    label2Key.mapNotNull { (label, key) -> target2Summary[key]?.let { label to it } }.toMap()

  val shortLabels: List<String> by lazy {
    summaries.map { it.id.toShortString(snapshot.repoMapping) }
  }

  val executableShortLabels: List<String> by lazy {
    summaries.filter { it.kind.isExecutable }
      .map { it.id.toShortString(snapshot.repoMapping) }
  }

  val allTestableSummaries: List<BuildTarget> by lazy {
    summaries.filter { it.kind.ruleType == RuleType.TEST && !it.isManual }
  }

  fun targetsForPath(path: Path): List<Label> =
    snapshot.fileToTarget.getTargetsByFile(path)
      .filter { it in importKeys }
      .map { it.label }
      .distinct()

  fun executableTargetsFor(label: Label): List<Label> =
    label.toCanonicalLabelOrThis(snapshot.repoMapping)?.let { snapshot.executableTargets.executableTargetsFor(it) }.orEmpty()

  // small data cache to accelerate repeated bulk reads of the same target data
  private val dataCache = Caffeine.newBuilder()
    .maximumSize(64)
    .build<Pair<Label, Class<*>>, BuildTargetData>()

  fun <T : BuildTargetData> data(label: Label, type: Class<T>): T? {
    val cached = dataCache.get(label to type) {
      val key = label2Key[label] ?: return@get EmptyBuildTargetData
      val options = TargetLoadOptions(
        sections = setOf(TargetSection.INFO),
        targetData = BuildTargetLoadHint.Subset(setOf(type)),
      )
      snapshot.targets.findTargetByKey(key, options)?.data(type) ?: EmptyBuildTargetData
    }
    return if (cached === EmptyBuildTargetData) {
      null
    }
    else {
      type.cast(cached)
    }
  }
}

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class TargetStorage(private val project: Project, private val coroutineScope: CoroutineScope) {
  private val snapshotService = project.service<WorkspaceSnapshotService>()

  val views: StateFlow<CachedSnapshotView> = snapshotService.snapshot.mapStateIn(coroutineScope) {
    CachedSnapshotView(it)
  }

  suspend fun awaitLoaded() {
    snapshotService.currentSnapshot() // force load
    // compare with the latest snapshot: `views` is conflated and can skip the one that `currentSnapshot()` returned
    views.first { it.snapshot === snapshotService.snapshot.value }
  }

  private fun view(): CachedSnapshotView = views.value

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

  fun allTargets(): Sequence<Label> = allTargetSummaries().asSequence().map { it.id }

  fun allTargetSummaries(): List<BuildTarget> = view().summaries

  fun allTestableSummaries(): List<BuildTarget> = view().allTestableSummaries

  fun getTargetSummary(label: Label): BuildTarget? =
    label.toCanonicalLabelOrThis(project)?.let { view().label2Summary[it] }

  fun getTotalTargetCount(): Int = view().summaries.size

  fun getTargetsForPath(path: Path): List<Label> = view().targetsForPath(path)

  fun getTargetsForFile(file: VirtualFile): List<Label> = file.toNioPathOrNull()?.let { getTargetsForPath(it) } ?: emptyList()

  fun getExecutableTargetsForFile(file: VirtualFile): List<Label> {
    val path = file.toNioPathOrNull() ?: return emptyList()
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

  fun <T : BuildTargetData> getTargetDataForLabel(label: Label, type: Class<T>): T? =
    label.toCanonicalLabelOrThis(project)?.let { view().data(it, type) }

  fun getTotalFileCount(): Int = view().snapshot.fileToTarget.size

  fun repoMapping(): RepoMapping = view().snapshot.repoMapping

  fun resolveExecrootOutputLocation(location: OutputLocation): Path? =
    view().let { view -> view.outputLocationResolver.resolve(location, view.snapshot.repoMapping.getLocalRepositories()) }
}

@ApiStatus.Internal
inline fun <reified T : BuildTargetData> TargetStorage.getTargetDataForLabel(label: Label): T? = getTargetDataForLabel(label, T::class.java)

val Project.targetStorage: TargetStorage
  @ApiStatus.Internal
  get() = service<TargetStorage>()
