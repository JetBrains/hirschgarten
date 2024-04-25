package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.goide.vgo.project.workspaceModel.entities.VgoDependencyEntity
import com.goide.vgo.project.workspaceModel.entities.VgoStandaloneModuleEntity
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GoModule
import org.jetbrains.workspacemodel.entities.BspEntitySource
import kotlin.io.path.toPath

public interface GoEntitiesExtension {
  public fun prepareAllEntitiesForGoModule(
    entityToAdd: GoModule,
    goModule: ModuleEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): GoModuleEntities
}

public data class GoModuleEntities(
  val goModuleWorkspaceEntity: WorkspaceEntity,
  val goDependenciesWorkspaceEntity: List<WorkspaceEntity>,
  val goLibrariesWorkspaceEntity: List<WorkspaceEntity>,
)

private val ep = ExtensionPointName.create<GoEntitiesExtension>(
  "org.jetbrains.bsp.goEntitiesExtension",
)

public fun goEntitiesExtension(): GoEntitiesExtension? = ep.extensionList.firstOrNull()

public fun goEntitiesExtensionExists(): Boolean = ep.extensionList.isNotEmpty()

public class GoEntitiesExtensionImpl: GoEntitiesExtension {
  override fun prepareAllEntitiesForGoModule(
    entityToAdd: GoModule,
    goModule: ModuleEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): GoModuleEntities {
    val vgoModule = VgoStandaloneModuleEntity(
      moduleId = goModule.symbolicId,
      entitySource = BspEntitySource,
      importPath = entityToAdd.importPath,
      root = entityToAdd.root.toVirtualFileUrl(virtualFileUrlManager),
    )

    val vgoModuleDependencies = entityToAdd.goDependencies.map {
      VgoDependencyEntity(
        importPath = it.importPath,
        entitySource = BspEntitySource,
        isMainModule = false,
        internal = true,
      ) {
        this.module = vgoModule
        this.root = it.root.toVirtualFileUrl(virtualFileUrlManager)
      }
    }

    val vgoModuleLibraries = entityToAdd.goLibraries.map {
      VgoDependencyEntity(
        importPath = it.goImportPath ?: "",
        entitySource = BspEntitySource,
        isMainModule = false,
        internal = false,
      ) {
        this.module = vgoModule
        this.root = it.goRoot?.toPath()?.toVirtualFileUrl(virtualFileUrlManager)
      }
    }

    return GoModuleEntities(
      goModuleWorkspaceEntity = vgoModule,
      goDependenciesWorkspaceEntity = vgoModuleDependencies,
      goLibrariesWorkspaceEntity = vgoModuleLibraries,
    )
  }
}
