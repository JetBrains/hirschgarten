package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaModuleSettingsEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.KotlincOpts
import java.nio.file.Path

internal data class JvmJdkInfo(val name: String, val javaHome: String)

internal data class KotlinAddendum(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: KotlincOpts?
)

internal data class JavaModule(
  val module: Module,
  val baseDirContentRoot: ContentRoot,
  val sourceRoots: List<JavaSourceRoot>,
  val resourceRoots: List<JavaResourceRoot>,
  val moduleLevelLibraries: List<Library>?,
  val compilerOutput: Path?,
  val jvmJdkInfo: JvmJdkInfo?,
  val kotlinAddendum: KotlinAddendum?,
) : WorkspaceModelEntity()

internal class JavaModuleWithSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater =
      ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, calculateJavaModuleDependencies(entityToAdd))

    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    addJavaModuleSettingsEntity(
      builder = workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      entityToAdd = entityToAdd,
      moduleEntity = moduleEntity
    )

    if (entityToAdd.isRoot()) {
      val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)
      contentRootEntityUpdater.addEntity(entityToAdd.baseDirContentRoot, moduleEntity)
    } else {
      val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)
      entityToAdd.moduleLevelLibraries?.let { libraryEntityUpdater.addEntries(it, moduleEntity) }

      val javaSourceEntityUpdater = JavaSourceEntityUpdater(workspaceModelEntityUpdaterConfig)
      javaSourceEntityUpdater.addEntries(entityToAdd.sourceRoots, moduleEntity)

      val javaResourceEntityUpdater = JavaResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
      javaResourceEntityUpdater.addEntries(entityToAdd.resourceRoots, moduleEntity)
    }

    if (entityToAdd.module.languageIds.contains("kotlin")) {
      val kotlinFacetEntityUpdater = KotlinFacetEntityUpdater(workspaceModelEntityUpdaterConfig)
      kotlinFacetEntityUpdater.addEntity(entityToAdd, moduleEntity)
    }

    return moduleEntity
  }

  private fun calculateJavaModuleDependencies(entityToAdd: JavaModule): List<ModuleDependencyItem> =
    if (entityToAdd.jvmJdkInfo != null) {
      defaultDependencies + ModuleDependencyItem.SdkDependency(entityToAdd.jvmJdkInfo.name, "JavaSDK")
    }
    else defaultDependencies

  private fun addJavaModuleSettingsEntity(
          builder: MutableEntityStorage,
          entityToAdd: JavaModule,
          moduleEntity: ModuleEntity
  ) {
    if (entityToAdd.compilerOutput != null) {
      val compilerOutput =
              entityToAdd.
              compilerOutput.
              toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
      builder.addEntity(
        JavaModuleSettingsEntity(
          inheritedCompilerOutput = false,
          excludeOutput = true,
          entitySource = BspEntitySource
        ) {
          this.compilerOutput = compilerOutput
          this.compilerOutputForTests = null
          this.languageLevelId = null
          this.module = moduleEntity
        }
      )
    }
  }

  private fun JavaModule.isRoot(): Boolean =  // TODO - that is a temporary predicate
    sourceRoots.isEmpty() && resourceRoots.isEmpty() && baseDirContentRoot.excludedPaths.isNotEmpty()

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

    return moduleEntityUpdater.addEntity(entityToAdd.module)
  }
}

internal class JavaModuleUpdater(
  workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  private val javaModuleWithSourcesUpdater = JavaModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig)
  private val javaModuleWithoutSourcesUpdater = JavaModuleWithoutSourcesUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity =
    if (entityToAdd.doesntContainSourcesAndResources() && entityToAdd.containsJavaKotlinLanguageIds()) {
      javaModuleWithoutSourcesUpdater.addEntity(entityToAdd)
    } else {
      javaModuleWithSourcesUpdater.addEntity(entityToAdd)
    }

  private fun JavaModule.doesntContainSourcesAndResources() =
    this.sourceRoots.isEmpty() && this.resourceRoots.isEmpty()

  private fun JavaModule.containsJavaKotlinLanguageIds() =
    this.module.languageIds.any { it == "kotlin" || it == "java" }
}
