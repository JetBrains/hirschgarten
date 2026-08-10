package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.TargetSection
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import java.nio.file.Path

@ApiStatus.Internal
interface BuildTarget {
  val key: WorkspaceTargetKey
  val kind: TargetKind
  val loaded: TargetLoadOptions

  // TargetSection.INFO
  val baseDirectory: Path
  val generatorName: String?
  /**
   * From Bazel doc (https://bazel.build/reference/be/common-definitions)
   * manual tag will exclude the target from expansion of target pattern wildcards (..., :*, :all, etc.) and test_suite rules which do not
   * list the test explicitly when computing the set of top-level targets to build/run for the build, test, and coverage commands
   */
  val isManual: Boolean
  /**
   * Indicates if this target belongs to workspace, or counted as "external"
   */
  val isWorkspace: Boolean
  val isTestOnly: Boolean
  val tags: List<String>

  // TargetSection.DEPS
  val dependencies: List<DependencyLabel>

  // TargetSection.FILE_SETS
  val sources: SourceFileCollection
  val generatedSources: SourceFileCollection
  val resources: SourceFileCollection

  // language-specific data, selected by TargetLoadOptions.targetData
  val data: List<BuildTargetData>
}

@get:ApiStatus.Internal
val BuildTarget.id: Label
  get() = key.label

/** whether this target already carries every section, i.e. reading it triggers no further loading */
@get:ApiStatus.Internal
val BuildTarget.isFull: Boolean
  get() = loaded == TargetLoadOptions.ALL

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

