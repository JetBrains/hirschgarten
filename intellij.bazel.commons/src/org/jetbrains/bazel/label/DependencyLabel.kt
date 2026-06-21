package org.jetbrains.bazel.label

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.Dependency.DependencyType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.toWorkspaceTargetKey

// TODO: move to backend-only entity and use WorkspaceConfigurationId
@ApiStatus.Internal
data class DependencyLabel(
  val targetKey: WorkspaceTargetKey,
  val kind: DependencyLabelKind = DependencyLabelKind.COMPILE,
) {
  companion object {
    @ApiStatus.Obsolete
    fun parse(value: String, isRuntime: Boolean = false): DependencyLabel =
      DependencyLabel(
        targetKey = WorkspaceTargetKey(label = Label.parse(value)),
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
fun IntellijIdeInfo.Dependency.toDependencyLabel(): DependencyLabel =
  DependencyLabel(
    targetKey = target.toWorkspaceTargetKey(),
    kind = when (dependencyType) {
      DependencyType.EXPORTED_COMPILE_TIME -> DependencyLabelKind.EXPORTED_COMPILE_TIME
      DependencyType.RUNTIME -> DependencyLabelKind.RUNTIME
      DependencyType.TOOLCHAIN -> DependencyLabelKind.TOOLCHAIN
      else -> DependencyLabelKind.COMPILE
    },
  )


@ApiStatus.Internal
fun IntellijIdeInfo.Dependency.isCompile(): Boolean =
  dependencyType == DependencyType.COMPILE_TIME || dependencyType == DependencyType.EXPORTED_COMPILE_TIME
