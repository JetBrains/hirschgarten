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
  COMPILE, EXPORTED_COMPILE_TIME, RUNTIME, TOOLCHAIN
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
