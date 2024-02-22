package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelEntity
import java.net.URI
import kotlin.io.path.toPath

internal abstract class ModuleDetailsToModuleTransformer<out T : WorkspaceModelEntity>(
  targetsMap: Map<BuildTargetId, BuildTargetInfo>,
  moduleNameProvider: ModuleNameProvider,
) :
  WorkspaceModelEntityTransformer<ModuleDetails, T> {
  protected abstract val type: String

  val bspModuleDetailsToModuleTransformer = BspModuleDetailsToModuleTransformer(targetsMap, moduleNameProvider)

  abstract override fun transform(inputEntity: ModuleDetails): T

  protected abstract fun toGenericModuleInfo(inputEntity: ModuleDetails): GenericModuleInfo

  protected fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      // TODO https://youtrack.jetbrains.com/issue/BAZEL-635
      path = URI.create(inputEntity.target.baseDirectory ?: "file:///todo").toPath(),
      excludedPaths = inputEntity.outputPathUris.map { URI.create(it).toPath() },
    )
}
