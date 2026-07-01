package org.jetbrains.bsp.protocol
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import java.nio.file.Path

@ApiStatus.Internal
interface ExecutableTarget {
  val id: Label
  val kind: TargetKind
}

@ApiStatus.Internal
interface BuildTarget : ExecutableTarget {

  // TODO: migrate to WorkspaceTargetKey
  override val id: Label
  override val kind: TargetKind
  val baseDirectory: Path
  val data: List<BuildTargetData>

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
}

// TODO: move to backend-only code
@ApiStatus.Internal
data class RawBuildTarget(
  val key: WorkspaceTargetKey,
  val dependencies: List<DependencyLabel>,
  override val kind: TargetKind,
  val sources: SourceFileCollection,
  val generatedSources: SourceFileCollection,
  val resources: SourceFileCollection,
  override val baseDirectory: Path,
  override val data: List<BuildTargetData> = emptyList(),
  val generatorName: String? = null,
  override val isManual: Boolean = false,
  override val isWorkspace: Boolean = true,
  val isTestOnly: Boolean = false,
) : BuildTarget {
  override val id: Label
    get() = key.label
}

@get:ApiStatus.Internal
val RawBuildTarget.allSources: Sequence<Path>
  get() = sources.getFiles() + generatedSources.getFiles()

@ApiStatus.Internal
data class PartialBuildTarget(
  override val id: Label,
  override val kind: TargetKind,
  override val baseDirectory: Path,
  override val data: List<BuildTargetData> = emptyList(),
  override val isManual: Boolean,
  override val isWorkspace: Boolean
) : BuildTarget

// adding or removing new BuildTargetData should not cause cache invalidation, but still we don't want to write FQN per each target
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
// id should in 1-255 range
@ApiStatus.Internal
annotation class ClassDiscriminator(val id: Short)

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

