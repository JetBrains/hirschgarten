package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsPaths
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.KotlinModuleKind
import org.jetbrains.kotlin.config.serializeComponentPlatforms
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.workspaceModel.CompilerArgumentsSerializer
import org.jetbrains.kotlin.idea.workspaceModel.CompilerSettingsData
import org.jetbrains.kotlin.idea.workspaceModel.KotlinSettingsEntity
import org.jetbrains.kotlin.idea.workspaceModel.kotlinSettings
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.KotlinAddendum
import java.nio.file.Path

internal class KotlinFacetEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val projectBasePath: Path,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaModule, KotlinSettingsEntity> {
  override fun addEntity(entityToAdd: JavaModule, parentModuleEntity: ModuleEntity): KotlinSettingsEntity {
    val kotlinAddendum = entityToAdd.kotlinAddendum
    val compilerArguments = kotlinAddendum?.kotlincOptions?.toK2JVMCompilerArguments(entityToAdd, kotlinAddendum)
    val kotlinSettingsEntity =
      calculateKotlinSettingsEntity(entityToAdd, compilerArguments, parentModuleEntity, kotlinAddendum?.kotlincOptions)
    return addKotlinSettingsEntity(parentModuleEntity, kotlinSettingsEntity)
  }

  private fun List<String>.toK2JVMCompilerArguments(entityToAdd: JavaModule, kotlinAddendum: KotlinAddendum) =
    parseCommandLineArguments(K2JVMCompilerArguments::class, this).apply {
      languageVersion = kotlinAddendum.languageVersion
      apiVersion = kotlinAddendum.apiVersion
      autoAdvanceLanguageVersion = false
      autoAdvanceApiVersion = false
      friendPaths = entityToAdd.toJpsFriendPaths(projectBasePath).toTypedArray()
    }

  private fun calculateKotlinSettingsEntity(
    entityToAdd: JavaModule,
    compilerArguments: K2JVMCompilerArguments?,
    parentModuleEntity: ModuleEntity,
    kotlincOpts: List<String>?,
  ) = KotlinSettingsEntity(
    name = KotlinFacetType.NAME,
    moduleId = parentModuleEntity.symbolicId,
    sourceRoots = emptyList(),
    configFileItems = emptyList(),
    useProjectSettings = false,
    implementedModuleNames = emptyList(),
    dependsOnModuleNames = emptyList(), // Gradle specific
    additionalVisibleModuleNames = entityToAdd.toAssociateModules().toMutableSet(),
    sourceSetNames = emptyList(),
    isTestModule = entityToAdd.genericModuleInfo.capabilities.canTest,
    externalProjectId = "",
    isHmppEnabled = false,
    pureKotlinSourceFolders = emptyList(),
    kind = KotlinModuleKind.DEFAULT,
    entitySource = parentModuleEntity.entitySource,
    externalSystemRunTasks = emptyList(),
    version = KotlinFacetSettings.CURRENT_VERSION,
    flushNeeded = true
  ) {
    this.productionOutputPath = ""
    this.testOutputPath = ""
    this.compilerArguments = compilerArguments?.let {
      CompilerArgumentsSerializer.serializeToString(it)
    } ?: ""
    this.compilerSettings = CompilerSettingsData(
      additionalArguments = kotlincOpts?.joinToString(" ") ?: "",
      scriptTemplates = "",
      scriptTemplatesClasspath = "",
      copyJsLibraryFiles = false,
      outputDirectoryForJsLibraryFiles = "",
    )
    this.targetPlatform = compilerArguments?.jvmTarget?.let { jvmTargetString ->
      JvmTarget.fromString(jvmTargetString)?.let { jvmTarget ->
        JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget).serializeComponentPlatforms()
      }
    } ?: ""
  }

  private fun JavaModule.toJpsFriendPaths(projectBasePath: Path): List<String> {
    val associateModules = toAssociateModules()

    if (associateModules.isEmpty()) return listOf()

    return associateModules.map { module ->
      JpsPaths.getJpsCompiledProductionPath(projectBasePath, module).toString()
    }
  }

  private fun JavaModule.toAssociateModules(): Set<String> =
    this.genericModuleInfo.associates.map { it.moduleName }.toSet()

  private fun addKotlinSettingsEntity(
    parentModuleEntity: ModuleEntity,
    kotlinSettingsEntity: KotlinSettingsEntity.Builder,
  ): KotlinSettingsEntity {
    val updatedParentModuleEntity = workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.modifyModuleEntity(parentModuleEntity) {
      this.kotlinSettings += kotlinSettingsEntity
    }

    return updatedParentModuleEntity.kotlinSettings.last()
  }
}
