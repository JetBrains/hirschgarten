package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaModuleSettingsEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesJava
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesKotlin
import java.nio.file.Path

internal class JavaModuleWithSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val projectBasePath: Path,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {
  override fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater =
      ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, calculateJavaModuleDependencies(entityToAdd))

    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.genericModuleInfo)

    addJavaModuleSettingsEntity(
      builder = workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      entityToAdd = entityToAdd,
      moduleEntity = moduleEntity,
    )

    if (entityToAdd.isRoot(projectBasePath)) {
      // TODO https://youtrack.jetbrains.com/issue/BAZEL-664
    } else {
      val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)
      entityToAdd.moduleLevelLibraries?.let { libraryEntityUpdater.addEntries(it, moduleEntity) }

      val javaSourceEntityUpdater = JavaSourceEntityUpdater(workspaceModelEntityUpdaterConfig)
      javaSourceEntityUpdater.addEntries(entityToAdd.sourceRoots, moduleEntity)

      val javaResourceEntityUpdater = JavaResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
      javaResourceEntityUpdater.addEntries(entityToAdd.resourceRoots, moduleEntity)
    }

    if (entityToAdd.genericModuleInfo.languageIds.includesKotlin()) {
      val kotlinFacetEntityUpdater = KotlinFacetEntityUpdater(workspaceModelEntityUpdaterConfig)
      kotlinFacetEntityUpdater.addEntity(entityToAdd, moduleEntity)
    }

    return moduleEntity
  }

  private fun calculateJavaModuleDependencies(entityToAdd: JavaModule): List<ModuleDependencyItem> {
    val returnDependencies: MutableList<ModuleDependencyItem> = defaultDependencies.toMutableList()
    entityToAdd.jvmJdkName?.let {
      returnDependencies.add(ModuleDependencyItem.SdkDependency(entityToAdd.jvmJdkName, "JavaSDK"))
    }
    entityToAdd.scalaAddendum?.let { addendum ->
      returnDependencies.add(toModuleDependencyItemLibraryDependency(
        LibraryDependency(addendum.scalaSdkName, true),
        entityToAdd.genericModuleInfo.name
      ))
      addendum.scalaSdkLibraries.forEach {
        returnDependencies.add(toModuleDependencyItemLibraryDependency(
          LibraryDependency(it, true),
          entityToAdd.genericModuleInfo.name
        ))
      }
    }
    return returnDependencies
  }

  private fun addJavaModuleSettingsEntity(
    builder: MutableEntityStorage,
    entityToAdd: JavaModule,
    moduleEntity: ModuleEntity,
  ) {
    if (entityToAdd.compilerOutput != null) {
      val compilerOutput =
        entityToAdd.compilerOutput.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
      builder.addEntity(
        JavaModuleSettingsEntity(
          inheritedCompilerOutput = false,
          excludeOutput = true,
          entitySource = BspEntitySource,
        ) {
          this.compilerOutput = compilerOutput
          this.compilerOutputForTests = null
          this.languageLevelId = null
          this.module = moduleEntity
        },
      )
    }
  }

  private companion object {
    val defaultDependencies = listOf(
      ModuleDependencyItem.ModuleSourceDependency,
    )
  }
}

internal fun JavaModule.isRoot(projectBasePath: Path): Boolean = // TODO - that is a temporary predicate
  sourceRoots.isEmpty() && resourceRoots.isEmpty() && baseDirContentRoot?.path == projectBasePath

internal class JavaModuleWithoutSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {
  override fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig)

    return moduleEntityUpdater.addEntity(entityToAdd.genericModuleInfo)
  }
}

internal class JavaModuleUpdater(
  workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  projectBasePath: Path,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {
  private val javaModuleWithSourcesUpdater =
    JavaModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig, projectBasePath)
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
    with(genericModuleInfo.languageIds) {
      includesKotlin() || includesJava()
    }
}
