package org.jetbrains.magicmetamodel.impl.workspacemodel.impl

import com.intellij.workspaceModel.ide.JpsFileEntitySource
import com.intellij.workspaceModel.ide.JpsProjectConfigLocation
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import java.nio.file.Path

internal class WorkspaceModelUpdaterImpl(
  val workspaceModel: WorkspaceModel,
  val virtualFileUrlManager: VirtualFileUrlManager,
  projectBaseDir: Path,
) : WorkspaceModelUpdater {

  private val workspaceModelEntityUpdaterConfig = WorkspaceModelEntityUpdaterConfig(
    workspaceModel = workspaceModel,
    virtualFileUrlManager = virtualFileUrlManager,
    projectConfigSource = calculateProjectConfigSource(
      virtualFileUrlManager,
      projectBaseDir
    )
  )

  override fun loadModule(moduleDetails: ModuleDetails) {
    // TODO for now we are supporting only java modules
    val javaModule = ModuleDetailsToJavaModuleTransformer.transform(moduleDetails)

    val javaModuleUpdater = JavaModuleUpdater(workspaceModelEntityUpdaterConfig)
    javaModuleUpdater.addEntity(javaModule)
  }

  // TODO move?
  private fun calculateProjectConfigSource(
    virtualFileUrlManager: VirtualFileUrlManager,
    projectBaseDir: Path
  ): JpsFileEntitySource {
    val virtualProjectBaseDirPath = projectBaseDir.toVirtualFileUrl(virtualFileUrlManager)
    val virtualProjectIdeaDirPath = virtualProjectBaseDirPath.append(".idea/")
    val virtualProjectModulesDirPath = virtualProjectIdeaDirPath.append("modules/")

    val projectLocation = JpsProjectConfigLocation.DirectoryBased(virtualProjectBaseDirPath, virtualProjectIdeaDirPath)

    return JpsFileEntitySource.FileInDirectory(virtualProjectModulesDirPath, projectLocation)
  }
}
