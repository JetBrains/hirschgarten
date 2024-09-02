package org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.plugins.bsp.impl.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.ContentRoot
import org.jetbrains.plugins.bsp.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.WorkspaceModelEntity
import kotlin.io.path.toPath

internal abstract class ModuleDetailsToModuleTransformer<out T : WorkspaceModelEntity>(
  targetsMap: Map<BuildTargetIdentifier, BuildTargetInfo>,
  moduleNameProvider: TargetNameReformatProvider,
  libraryNameProvider: TargetNameReformatProvider,
) : WorkspaceModelEntityTransformer<ModuleDetails, T> {
  protected abstract val type: ModuleTypeId

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
