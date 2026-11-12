package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey

// TODO: make concrete data class in 2027.1, keep it sealed
@ApiStatus.Internal
sealed interface BuildTarget {
  val info: BuildTargetInfo
  val dependencies: List<DependencyLabel>
  val sources: OutputLocationCollection
  val resources: OutputLocationCollection
  val data: List<BuildTargetData>

  val key: WorkspaceTargetKey
    get() = info.key

  val kind: TargetKind
    get() = info.kind

  val generatorName: String?
    get() = info.generatorName

  val isTestOnly: Boolean
    get() = info.isTestOnly

  val tags: List<String>
    get() = info.tags

  val workspaceName: String
    get() = info.workspaceName

  @Deprecated("BuildTarget always has all its sections", level = DeprecationLevel.ERROR)
  val loaded: TargetLoadOptions
    get() = TargetLoadOptions.ALL
}

private data class BuildTargetImpl(
  override val info: BuildTargetInfo,
  override val dependencies: List<DependencyLabel>,
  override val sources: OutputLocationCollection,
  override val resources: OutputLocationCollection,
  override val data: List<BuildTargetData>,
) : BuildTarget

@ApiStatus.Internal
fun BuildTarget(
  info: BuildTargetInfo,
  dependencies: List<DependencyLabel>,
  sources: OutputLocationCollection,
  resources: OutputLocationCollection,
  data: List<BuildTargetData>,
): BuildTarget = BuildTargetImpl(info, dependencies, sources, resources, data)

@ApiStatus.Internal
fun BuildTarget.copy(
  info: BuildTargetInfo = this.info,
  dependencies: List<DependencyLabel> = this.dependencies,
  sources: OutputLocationCollection = this.sources,
  resources: OutputLocationCollection = this.resources,
  data: List<BuildTargetData> = this.data,
): BuildTarget = BuildTarget(info, dependencies, sources, resources, data)

@get:ApiStatus.Internal
val BuildTarget.id: Label
  get() = key.label

@ApiStatus.Internal
fun <T : BuildTargetData> BuildTarget.data(type: Class<T>): T? =
  data.firstOrNull { type.isInstance(it) }?.let { type.cast(it) }

@ApiStatus.Internal
interface BuildTargetData

@ApiStatus.Internal
enum class StrictDependencyCheckedType {
  OFF,
  WARNING,
  ERROR,
}

@ApiStatus.Internal
inline fun <reified Data> BuildTarget.extractData(): Data? = this.data.filterIsInstance<Data>().singleOrNull()

@get:ApiStatus.Internal
val BuildTarget.isManual: Boolean
  get() = info.isManual

@ApiStatus.Internal
fun BuildTarget.nonGeneratedSources(): Sequence<OutputLocation> = sources.getOutputLocations().filter { it.isSource }
