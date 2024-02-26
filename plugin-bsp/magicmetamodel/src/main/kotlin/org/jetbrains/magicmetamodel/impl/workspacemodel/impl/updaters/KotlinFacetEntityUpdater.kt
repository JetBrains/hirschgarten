package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
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
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.KotlinAddendum
import java.nio.file.Path

internal class KotlinFacetEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val projectBasePath: Path,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaModule, KotlinSettingsEntity> {
  override fun addEntity(entityToAdd: JavaModule, parentModuleEntity: ModuleEntity): KotlinSettingsEntity {
    val kotlinAddendum = entityToAdd.kotlinAddendum
    val compilerArguments = kotlinAddendum?.kotlincOptions?.toK2JVMCompilerArguments(kotlinAddendum)
    val kotlinSettingsEntity =
      calculateKotlinSettingsEntity(entityToAdd, compilerArguments, parentModuleEntity)
    return addKotlinSettingsEntity(kotlinSettingsEntity)
  }

  private fun List<String>.toK2JVMCompilerArguments(kotlinAddendum: KotlinAddendum) =
    parseCommandLineArguments(K2JVMCompilerArguments::class, this).apply {
      languageVersion = kotlinAddendum.languageVersion
      apiVersion = kotlinAddendum.apiVersion
      autoAdvanceLanguageVersion = false
      autoAdvanceApiVersion = false
    }

  private fun calculateKotlinSettingsEntity(
    entityToAdd: JavaModule,
    compilerArguments: K2JVMCompilerArguments?,
    parentModuleEntity: ModuleEntity,
  ) = KotlinSettingsEntity(
    name = KotlinFacetType.NAME,
    moduleId = parentModuleEntity.symbolicId,
    sourceRoots = emptyList(),
    configFileItems = emptyList(),
    useProjectSettings = false,
    implementedModuleNames = emptyList(),
    dependsOnModuleNames = emptyList(), // Gradle specific
    additionalVisibleModuleNames = entityToAdd.toAssociateModules().toMutableSet(),
    productionOutputPath = "",
    testOutputPath = "",
    sourceSetNames = emptyList(),
    isTestModule = entityToAdd.genericModuleInfo.capabilities.canTest,
    externalProjectId = "",
    isHmppEnabled = false,
    pureKotlinSourceFolders = emptyList(),
    kind = KotlinModuleKind.DEFAULT,
    compilerArguments = compilerArguments?.let {
      CompilerArgumentsSerializer.serializeToString(it)
    } ?: "",
    compilerSettings = CompilerSettingsData(
      additionalArguments = entityToAdd.toFriendPaths(projectBasePath),
      scriptTemplates = "",
      scriptTemplatesClasspath = "",
      copyJsLibraryFiles = false,
      outputDirectoryForJsLibraryFiles = "",
      isInitialized = true
    ),
    targetPlatform = compilerArguments?.jvmTarget?.let { jvmTargetString ->
      JvmTarget.fromString(jvmTargetString)?.let { jvmTarget ->
        JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget).serializeComponentPlatforms()
      }
    } ?: "",
    entitySource = parentModuleEntity.entitySource,
    externalSystemRunTasks = emptyList(),
    version = KotlinFacetSettings.CURRENT_VERSION,
    flushNeeded = true
  ) {
    module = parentModuleEntity
  }

  private fun JavaModule.toFriendPaths(projectBasePath: Path): String {
    val associateModules = toAssociateModules()

    if (associateModules.isEmpty()) return ""

    val friendPaths = associateModules.map { module ->
      JpsPaths.getJpsCompiledProductionDirectory(projectBasePath, module)
    }

    return "-Xfriend-paths=${friendPaths.joinToString(",")}"
  }

  private fun JavaModule.toAssociateModules(): Set<String> =
    this.genericModuleInfo.associates.map { it.moduleName }.toSet()

  private fun addKotlinSettingsEntity(
    kotlinSettingsEntity: KotlinSettingsEntity,
  ): KotlinSettingsEntity {
    return workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(
      kotlinSettingsEntity
    )
  }
}
