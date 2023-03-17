package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleDependencyItem
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addJavaModuleSettingsEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import java.nio.file.Path

internal data class JvmJdkInfo(val javaVersion: String, val javaHome: String)

internal data class JavaModule(
  val module: Module,
  val baseDirContentRoot: ContentRoot,
  val sourceRoots: List<JavaSourceRoot>,
  val resourceRoots: List<JavaResourceRoot>,
  val libraries: List<Library>,
  val compilerOutput: Path?,
  val jvmJdkInfo: JvmJdkInfo?,
) : WorkspaceModelEntity()

internal class JavaModuleWithSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, calculateJavaModuleDependencies(entityToAdd))

    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    addJavaModuleSettingsEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd, moduleEntity)

    val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)
    libraryEntityUpdater.addEntries(entityToAdd.libraries, moduleEntity)

    val javaSourceEntityUpdater = JavaSourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    javaSourceEntityUpdater.addEntries(entityToAdd.sourceRoots, moduleEntity)

    val javaResourceEntityUpdater = JavaResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    javaResourceEntityUpdater.addEntries(entityToAdd.resourceRoots, moduleEntity)

    return moduleEntity
  }

  private fun calculateJavaModuleDependencies(entityToAdd: JavaModule): List<ModuleDependencyItem> =
    if (entityToAdd.jvmJdkInfo != null) {
      defaultDependencies + ModuleDependencyItem.SdkDependency(entityToAdd.jvmJdkInfo.javaVersion, "JavaSDK")
    }
    else defaultDependencies

  private fun addJavaModuleSettingsEntity(builder: MutableEntityStorage, entityToAdd: JavaModule, moduleEntity: ModuleEntity) {
    if (entityToAdd.compilerOutput != null) {
      builder.addJavaModuleSettingsEntity(
        inheritedCompilerOutput = false,
        excludeOutput = true,
        compilerOutput = entityToAdd.compilerOutput.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        compilerOutputForTests = null,
        languageLevelId = null,
        module = moduleEntity,
        source = DoNotSaveInDotIdeaDirEntitySource,
      )
    }
  }

  private companion object {
    val defaultDependencies = listOf(
      ModuleDependencyItem.ModuleSourceDependency,
    )
  }
}

internal class JavaModuleWithoutSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig)
    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)
    contentRootEntityUpdater.addEntity(entityToAdd.baseDirContentRoot, moduleEntity)

    return moduleEntity
  }
}

internal class JavaModuleUpdater(
  workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  private val javaModuleWithSourcesUpdater = JavaModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig)
  private val javaModuleWithoutSourcesUpdater = JavaModuleWithoutSourcesUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity =
    when (Pair(entityToAdd.sourceRoots.size, entityToAdd.resourceRoots.size)) {
      Pair(0, 0) -> javaModuleWithoutSourcesUpdater.addEntity(entityToAdd)
      else -> javaModuleWithSourcesUpdater.addEntity(entityToAdd)
    }
}
