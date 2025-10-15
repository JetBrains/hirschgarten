package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaModuleSettingsEntity
import com.intellij.java.workspace.entities.javaSettings
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.pom.java.LanguageLevel
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.jpsCompilation.utils.JpsPaths
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.scalaVersionToScalaSdkName
import org.jetbrains.bazel.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Library
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Module
import java.nio.file.Path

class JavaModuleWithSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val projectBasePath: Path,
  moduleEntities: List<Module>,
  private val libraries: Map<String, Library>,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {
  val packageMarkerEntityUpdater =
    PackageMarkerEntityUpdater(
      workspaceModelEntityUpdaterConfig,
      moduleEntities,
    )

  override suspend fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater =
      ModuleEntityUpdater(
        workspaceModelEntityUpdaterConfig,
        calculateJavaModuleDependencies(entityToAdd),
        libraries,
        entityToAdd.runtimeDependencies,
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

    if (entityToAdd.genericModuleInfo.isDummy && BazelFeatureFlags.fbsrSupportedInPlatform) {
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
        entityToAdd = entityToAdd,
        parentModuleEntity = moduleEntity,
        projectBasePath = projectBasePath,
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
        returnDependencies.add(toLibraryDependency(addendum.scalaVersion.scalaVersionToScalaSdkName(), exported = false))
      }
    }

    return returnDependencies
  }

  private fun addJavaModuleSettingsEntity(
    builder: MutableEntityStorage,
    entityToAdd: JavaModule,
    moduleEntity: ModuleEntity,
  ) {
    val compilerOutput =
      JpsPaths
        .getJpsCompiledProductionPath(projectBasePath, entityToAdd.genericModuleInfo.name)
        .toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
    val testCompilerOutput =
      JpsPaths
        .getJpsCompiledTestPath(projectBasePath, entityToAdd.genericModuleInfo.name)
        .toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
    val entity =
      JavaModuleSettingsEntity(
        inheritedCompilerOutput = false,
        excludeOutput = true,
        entitySource = moduleEntity.entitySource,
      ) {
        this.compilerOutput = compilerOutput
        this.compilerOutputForTests = testCompilerOutput
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

class JavaModuleWithoutSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val libraries: Map<String, Library> = emptyMap(),
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {
  override suspend fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater =
      ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, calculateJavaModuleDependencies(entityToAdd), libraries, entityToAdd.runtimeDependencies)

    return moduleEntityUpdater.addEntity(entityToAdd.genericModuleInfo)
  }

  private fun calculateJavaModuleDependencies(entityToAdd: JavaModule): List<ModuleDependencyItem> =
    entityToAdd.jvmJdkName
      ?.let {
        listOf(SdkDependency(SdkId(it, "JavaSDK")))
      } ?: listOf()
}

class JavaModuleUpdater(
  workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  projectBasePath: Path,
  moduleEntities: List<Module> = emptyList(),
  libraries: List<Library> = emptyList(),
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {
  private val librariesByName = libraries.associateBy { it.displayName }
  private val javaModuleWithSourcesUpdater =
    JavaModuleWithSourcesUpdater(
      workspaceModelEntityUpdaterConfig,
      projectBasePath,
      moduleEntities,
      librariesByName,
    )
  private val javaModuleWithoutSourcesUpdater =
    JavaModuleWithoutSourcesUpdater(workspaceModelEntityUpdaterConfig, librariesByName)

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

// TODO: should be removed, kotlin needs a separate sync hook: https://youtrack.jetbrains.com/issue/BAZEL-1885
interface KotlinFacetEntityUpdater {
  fun addEntity(
    diff: MutableEntityStorage,
    entityToAdd: JavaModule,
    parentModuleEntity: ModuleEntity,
    projectBasePath: Path,
  )

  companion object {
    val ep = ExtensionPointName.create<KotlinFacetEntityUpdater>("org.jetbrains.bazel.kotlinFacetEntityUpdater")
  }
}
