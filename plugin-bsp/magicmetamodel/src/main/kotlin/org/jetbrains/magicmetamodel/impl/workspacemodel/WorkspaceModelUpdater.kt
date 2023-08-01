package org.jetbrains.magicmetamodel.impl.workspacemodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.WorkspaceModelUpdaterImpl
import java.nio.file.Path

internal data class ModuleDetails(
  val target: BuildTarget,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val dependenciesSources: List<DependencySourcesItem>,
  val javacOptions: JavacOptionsItem?,
  val pythonOptions: PythonOptionsItem?,
  val outputPathUris: List<String>,
  val libraryDependencies: List<BuildTargetId>?,
  val moduleDependencies: List<BuildTargetId>,
)

internal data class ModuleName(
  val name: String,
)

internal interface WorkspaceModelUpdater {

  fun loadModules(moduleEntities: List<Module>) =
    moduleEntities.forEach { loadModule(it) }

  fun loadModule(module: Module)

  fun loadLibraries(libraries: List<Library>)

  fun removeModules(modules: List<ModuleName>) =
    modules.forEach { removeModule(it) }

  fun removeModule(module: ModuleName)

  fun clear()

  companion object {
    fun create(
      workspaceEntityStorageBuilder: MutableEntityStorage,
      virtualFileUrlManager: VirtualFileUrlManager,
      projectBasePath: Path,
    ): WorkspaceModelUpdater =
      WorkspaceModelUpdaterImpl(
        workspaceEntityStorageBuilder = workspaceEntityStorageBuilder,
        virtualFileUrlManager = virtualFileUrlManager,
        projectBasePath = projectBasePath
      )
  }
}
