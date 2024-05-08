package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.WorkspaceModelEntity
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import kotlin.io.path.toPath

internal abstract class ModuleDetailsToModuleTransformer<out T : WorkspaceModelEntity>(
  targetsMap: Map<BuildTargetId, BuildTargetInfo>,
  moduleNameProvider: TargetNameReformatProvider,
  libraryNameProvider: TargetNameReformatProvider,
) :
  WorkspaceModelEntityTransformer<ModuleDetails, T> {
  protected abstract val type: String

  val bspModuleDetailsToModuleTransformer =
    BspModuleDetailsToModuleTransformer(targetsMap, moduleNameProvider, libraryNameProvider)

  abstract override fun transform(inputEntity: ModuleDetails): T

  protected abstract fun toGenericModuleInfo(inputEntity: ModuleDetails): GenericModuleInfo

  protected fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      // TODO https://youtrack.jetbrains.com/issue/BAZEL-635
      path = (inputEntity.target.baseDirectory ?: "file:///todo").safeCastToURI().toPath(),
      excludedPaths = inputEntity.outputPathUris.map { it.safeCastToURI().toPath() },
    )
}
