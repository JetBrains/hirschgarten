package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.utils.safeCastToURI
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelEntity
import kotlin.io.path.toPath

internal abstract class ModuleDetailsToModuleTransformer<out T : WorkspaceModelEntity>(
  targetsMap: Map<Label, BuildTargetInfo>,
  nameProvider: TargetNameReformatProvider,
) : WorkspaceModelEntityTransformer<ModuleDetails, T> {
  protected abstract val type: ModuleTypeId

  val bspModuleDetailsToModuleTransformer =
    BspModuleDetailsToModuleTransformer(targetsMap, nameProvider)

  abstract override fun transform(inputEntity: ModuleDetails): T

  protected abstract fun toGenericModuleInfo(inputEntity: ModuleDetails): GenericModuleInfo

  protected fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      // TODO https://youtrack.jetbrains.com/issue/BAZEL-635
      path = (inputEntity.target.baseDirectory ?: "file:///todo").safeCastToURI().toPath(),
      excludedPaths = inputEntity.outputPathUris.map { it.safeCastToURI().toPath() },
    )
}
