package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bsp.protocol.BuildTarget

data class BspModuleDetails(
  val target: BuildTarget,
  val javacOptions: List<String>,
  val type: ModuleTypeId,
  val associates: List<Label> = listOf(),
  val dependencies: List<Label>,
)

class BspModuleDetailsToModuleTransformer(private val targetsMap: Map<Label, BuildTarget>, private val project: Project) :
  WorkspaceModelEntityTransformer<BspModuleDetails, GenericModuleInfo> {
  override fun transform(inputEntity: BspModuleDetails): GenericModuleInfo =
    GenericModuleInfo(
      name = inputEntity.target.id.formatAsModuleName(project),
      type = inputEntity.type,
      dependencies = inputEntity.dependencies.map { it.formatAsModuleName(project) },
      kind = inputEntity.target.kind,
      associates =
        inputEntity.associates.mapNotNull {
          targetsMap[it]?.id?.formatAsModuleName(project)
        },
    )
}
