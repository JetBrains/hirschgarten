package org.jetbrains.bazel.label

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.Dependency.DependencyType

// TODO: move to backend-only entity and use WorkspaceConfigurationId
@ApiStatus.Internal
data class DependencyLabel(
  val label: Label,
  val configuration: String? = null,
  val kind: DependencyLabelKind = DependencyLabelKind.COMPILE,
) {
  companion object {
    @ApiStatus.Obsolete
    fun parse(value: String, isRuntime: Boolean = false): DependencyLabel =
      DependencyLabel(
        Label.parse(value),
        kind = if (isRuntime) {
          DependencyLabelKind.RUNTIME
        }
        else {
          DependencyLabelKind.COMPILE
        },
      )
  }

  val isRuntime: Boolean
    get() = kind == DependencyLabelKind.RUNTIME

  val exported: Boolean
    get() = kind == DependencyLabelKind.EXPORTED_COMPILE_TIME
}

@ApiStatus.Internal
enum class DependencyLabelKind {
  COMPILE,

  /**
   * A compile-scope dependency that is never exported, even for rule kinds whose module dependencies are
   * otherwise force-exported during resolution. Used for dependencies synthesized from a target's jdeps
   * that point at another imported target: the consumer must be able to resolve the producer's classes,
   * but it does not declare or re-export the producer, so the producer must not leak to the consumer's
   * own dependents.
   */
  COMPILE_NON_EXPORTED,
  EXPORTED_COMPILE_TIME,
  RUNTIME,
  TOOLCHAIN,
}

@ApiStatus.Internal
fun BspTargetInfo.Dependency.toDependencyLabel(): DependencyLabel =
  DependencyLabel(
    label = label(),
    kind = when (dependencyType) {
      DependencyType.EXPORTED_COMPILE_TIME -> DependencyLabelKind.EXPORTED_COMPILE_TIME
      DependencyType.RUNTIME -> DependencyLabelKind.RUNTIME
      DependencyType.TOOLCHAIN -> DependencyLabelKind.TOOLCHAIN
      else -> DependencyLabelKind.COMPILE
    },
    configuration = this.target.configuration.ifEmpty { null }
  )


@ApiStatus.Internal
fun BspTargetInfo.Dependency.isCompile(): Boolean =
  dependencyType == DependencyType.COMPILE || dependencyType == DependencyType.EXPORTED_COMPILE_TIME
