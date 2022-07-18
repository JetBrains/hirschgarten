package org.jetbrains.magicmetamodel.impl.workspacemodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.storage.WorkspaceEntityStorageBuilder
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

internal data class ModuleName(
  val name: String,
)

internal interface WorkspaceModelUpdater {

//  fun loadRootModule()

  fun loadModules(modulesDetails: List<ModuleDetails>) =
    modulesDetails.forEach(this::loadModule)

  fun loadModule(moduleDetails: ModuleDetails)

  fun removeModules(modules: List<ModuleName>) =
    modules.forEach(this::removeModule)

  fun removeModule(module: ModuleName)

  fun clear()

  companion object {
    fun create(
      workspaceEntityStorageBuilder: WorkspaceEntityStorageBuilder,
      virtualFileUrlManager: VirtualFileUrlManager,
      projectBaseDir: Path,
    ): WorkspaceModelUpdater =
      WorkspaceModelUpdaterImpl(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBaseDir)
  }
}
