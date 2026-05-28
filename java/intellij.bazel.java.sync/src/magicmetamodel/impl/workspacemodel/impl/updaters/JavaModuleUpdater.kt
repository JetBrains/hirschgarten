
package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaModuleSettingsEntity
import com.intellij.java.workspace.entities.javaSettings
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.pom.java.LanguageLevel
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.scalaVersionToScalaSdkName
import org.jetbrains.bazel.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.bazel.workspace.importer.KotlinFacetEntityUpdater
import org.jetbrains.bazel.workspace.importer.KotlinOptions
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bazel.workspacemodel.entities.Module

@ApiStatus.Internal
class JavaModuleWithSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val modules: Map<String, Module>,
  private val libraries: Map<String, Library>,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {
  private val packageMarkerEntityUpdater =
    PackageMarkerEntityUpdater(
      workspaceModelEntityUpdaterConfig,
      modules.values.toList(),
    )

  override suspend fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater =
      ModuleEntityUpdater(
        workspaceModelEntityUpdaterConfig = workspaceModelEntityUpdaterConfig,
        defaultDependencies = calculateJavaModuleDependencies(entityToAdd),
        modules = modules,
        libraries = libraries,
      )

    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.genericModuleInfo)

    addJavaModuleSettingsEntity(
      builder = workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      entityToAdd = entityToAdd,
      moduleEntity = moduleEntity,
    )

    entityToAdd.scalaAddendum?.let {
      val scalaAddendumEntityUpdater = ScalaAddendumEntityUpdater(workspaceModelEntityUpdaterConfig)
      scalaAddendumEntityUpdater.addEntity(it, moduleEntity)
    }

    if (entityToAdd.genericModuleInfo.isDummy) {
      packageMarkerEntityUpdater.addEntities(entityToAdd.sourceRoots, moduleEntity)
    } else {
      val javaSourceEntityUpdater =
        JavaSourceEntityUpdater(
          workspaceModelEntityUpdaterConfig,
          entityToAdd.workspaceModelEntitiesFolderMarker,
        )
      javaSourceEntityUpdater.addEntities(entityToAdd.sourceRoots, moduleEntity)
    }

    val javaResourceEntityUpdater = JavaResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    javaResourceEntityUpdater.addEntities(entityToAdd.resourceRoots, moduleEntity)

    if (entityToAdd.jvmBinaryJars.isNotEmpty()) {
      val jvmBinaryJarsEntityUpdater = JvmBinaryJarsEntityUpdater(workspaceModelEntityUpdaterConfig)
      jvmBinaryJarsEntityUpdater.addEntity(entityToAdd, moduleEntity)
    }

    if (entityToAdd.genericModuleInfo.kind.includesKotlin()) {
      KotlinFacetEntityUpdater.ep.extensionList.firstOrNull()?.addEntity(
        diff = workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
        parentModuleEntity = moduleEntity,
        kotlinOptions = entityToAdd.kotlinAddendum?.let {
          KotlinOptions(
            languageVersion = it.languageVersion,
            apiVersion = it.apiVersion,
            moduleName = it.moduleName,
            kotlincOptions = it.kotlincOptions,
          )
        },
        isTestModule = entityToAdd.genericModuleInfo.kind.ruleType == RuleType.TEST,
        associates = entityToAdd.genericModuleInfo.associates.toSet(),
      )
    }

    return moduleEntity
  }

  private fun calculateJavaModuleDependencies(entityToAdd: JavaModule): List<ModuleDependencyItem> {
    val returnDependencies: MutableList<ModuleDependencyItem> = defaultDependencies.toMutableList()
    entityToAdd.jvmJdkName?.also {
      returnDependencies.add(SdkDependency(SdkId(it, "JavaSDK")))
    }
    if (scalaSdkExtensionExists()) {
      entityToAdd.scalaAddendum?.also { addendum ->
        returnDependencies.add(toLibraryDependency(
          addendum.scalaVersion.scalaVersionToScalaSdkName(), exported = false
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
    val entity =
      JavaModuleSettingsEntity(
        inheritedCompilerOutput = false,
        excludeOutput = true,
        entitySource = moduleEntity.entitySource,
      ) {
        this.languageLevelId = LanguageLevel.parse(entityToAdd.javaAddendum?.languageVersion)?.name
      }

    builder.modifyModuleEntity(moduleEntity) {
      this.javaSettings = entity
    }
  }

  private companion object {
    val defaultDependencies =
      listOf(
        ModuleSourceDependency,
      )
  }
}

@ApiStatus.Internal
class JavaModuleWithoutSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val modules: Map<String, Module>,
  private val libraries: Map<String, Library> = emptyMap(),
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {
  override suspend fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater =
      ModuleEntityUpdater(
        workspaceModelEntityUpdaterConfig,
        calculateJavaModuleDependencies(entityToAdd),
        modules,
        libraries,
      )

    return moduleEntityUpdater.addEntity(entityToAdd.genericModuleInfo)
  }

  private fun calculateJavaModuleDependencies(entityToAdd: JavaModule): List<ModuleDependencyItem> =
    entityToAdd.jvmJdkName
      ?.let {
        listOf(SdkDependency(SdkId(it, "JavaSDK")))
      } ?: listOf()
}

@ApiStatus.Internal
class JavaModuleUpdater(
  workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  moduleEntities: List<Module> = emptyList(),
  libraries: List<Library> = emptyList(),
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {
  private val modulesByName = moduleEntities.associateBy { it.getModuleName() }
  private val librariesByName = libraries.associateBy { it.displayName }
  private val javaModuleWithSourcesUpdater =
    JavaModuleWithSourcesUpdater(
      workspaceModelEntityUpdaterConfig,
      modulesByName,
      librariesByName,
    )
  private val javaModuleWithoutSourcesUpdater =
    JavaModuleWithoutSourcesUpdater(workspaceModelEntityUpdaterConfig, modulesByName, librariesByName)

  override suspend fun addEntity(entityToAdd: JavaModule): ModuleEntity? =
    if (entityToAdd.doesntContainSourcesAndResources() && entityToAdd.containsJavaKotlinLanguageIds()) {
      javaModuleWithoutSourcesUpdater.addEntity(entityToAdd)
    } else if (entityToAdd.containsKotlinLanguageId()) {
      entityToAdd.addKotlinModuleIfPossible()
    } else {
      javaModuleWithSourcesUpdater.addEntity(entityToAdd)
    }

  private fun JavaModule.doesntContainSourcesAndResources() = this.sourceRoots.isEmpty() && this.resourceRoots.isEmpty()

  private fun JavaModule.containsJavaKotlinLanguageIds() =
    with(genericModuleInfo.kind) {
      includesKotlin() || includesJava()
    }

  private fun JavaModule.containsKotlinLanguageId() = genericModuleInfo.kind.includesKotlin()

  private suspend fun JavaModule.addKotlinModuleIfPossible(): ModuleEntity? =
    if (KotlinFacetEntityUpdater.ep.extensionList.isNotEmpty()) javaModuleWithSourcesUpdater.addEntity(this) else null
}
