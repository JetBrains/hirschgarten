package org.jetbrains.magicmetamodel.impl.workspacemodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.WorkspaceModelUpdaterImpl
import java.nio.file.Path

internal data class ModuleDetails(
  val target: BuildTarget,
  val allTargetsIds: List<BuildTargetIdentifier>,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val dependenciesSources: List<DependencySourcesItem>,
)

internal interface WorkspaceModelUpdater {

//  fun loadRootModule()

  fun loadModules(modulesDetails: List<ModuleDetails>) =
    modulesDetails.forEach(this::loadModule)

  fun loadModule(moduleDetails: ModuleDetails)

//  fun removeModule(module: Any)

//  fun clear()

  companion object {
    fun create(
      workspaceModel: WorkspaceModel,
      virtualFileUrlManager: VirtualFileUrlManager,
      projectBaseDir: Path,
    ): WorkspaceModelUpdater =
      WorkspaceModelUpdaterImpl(workspaceModel, virtualFileUrlManager, projectBaseDir)
  }
}
