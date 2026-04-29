package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.workspacemodel.entities.Dependency
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
data class BspModuleDetails(
  val target: BuildTarget,
  val javacOptions: List<String>,
  val type: ModuleTypeId,
  val associates: List<Label> = listOf(),
  val dependencies: List<DependencyLabel>,
)

@ApiStatus.Internal
class BspModuleDetailsToModuleTransformer(private val targetsMap: Map<Label, BuildTarget>,
                                          private val repoMapping: RepoMapping) :
  WorkspaceModelEntityTransformer<BspModuleDetails, GenericModuleInfo> {
  override fun transform(inputEntity: BspModuleDetails): GenericModuleInfo =
    GenericModuleInfo(
      label = inputEntity.target.id,
      name = inputEntity.target.id.formatAsModuleName(repoMapping),
      type = inputEntity.type,
      dependencies = inputEntity.dependencies.map { Dependency(it.label.formatAsModuleName(repoMapping), it.isRuntime, it.exported) },
      kind = inputEntity.target.kind,
      associates =
        inputEntity.associates.mapNotNull {
          targetsMap[it]?.id?.formatAsModuleName(repoMapping)
        },
    )
}
