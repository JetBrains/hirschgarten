package org.jetbrains.bazel.sync

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.workspace.importer.KotlinFacetEntityUpdater
import org.jetbrains.bazel.workspace.importer.KotlinOptions
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
import org.jetbrains.kotlin.idea.workspaceModel.KotlinSettingsEntityBuilder
import org.jetbrains.kotlin.idea.workspaceModel.kotlinSettings
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

@ApiStatus.Internal
class BazelKotlinFacetEntityUpdater : KotlinFacetEntityUpdater {
  override fun addEntity(
    diff: MutableEntityStorage,
    parentModuleEntity: ModuleEntity,
    kotlinOptions: KotlinOptions?,
    isTestModule: Boolean,
    associates: Set<String>,
  ) {
    val compilerArguments = kotlinOptions?.kotlincOptions?.toK2JVMCompilerArguments(kotlinOptions)
    val kotlinSettingsEntity =
      calculateKotlinSettingsEntity(compilerArguments, parentModuleEntity, isTestModule, associates)
    diff.addKotlinSettingsEntity(parentModuleEntity, kotlinSettingsEntity)
  }

  private fun List<String>.toK2JVMCompilerArguments(
    kotlinOptions: KotlinOptions,
  ) = parseCommandLineArguments(K2JVMCompilerArguments::class, this).apply {
    kotlinOptions.languageVersion?.let { languageVersion = it }
    kotlinOptions.apiVersion?.let { apiVersion = it }
    moduleName = kotlinOptions.moduleName

    autoAdvanceLanguageVersion = false
    autoAdvanceApiVersion = false
  }

  private fun calculateKotlinSettingsEntity(
    compilerArguments: K2JVMCompilerArguments?,
    parentModuleEntity: ModuleEntity,
    isTestModule: Boolean,
    associates: Set<String>,
  ) = KotlinSettingsEntity(
    moduleId = parentModuleEntity.symbolicId,
    name = KotlinFacetType.NAME,
    sourceRoots = emptyList(),
    configFileItems = emptyList(),
    useProjectSettings = false,
    implementedModuleNames = emptyList(),
    dependsOnModuleNames = emptyList(), // Gradle specific
    additionalVisibleModuleNames = associates.toMutableSet(),
    sourceSetNames = emptyList(),
    isTestModule = isTestModule,
    externalProjectId = "",
    isHmppEnabled = false,
    pureKotlinSourceFolders = emptyList(),
    kind = KotlinModuleKind.DEFAULT,
    externalSystemRunTasks = emptyList(),
    version = KotlinFacetSettings.CURRENT_VERSION,
    flushNeeded = true,
    entitySource = parentModuleEntity.entitySource,
  ) {
    this.productionOutputPath = ""
    this.testOutputPath = ""
    this.compilerArguments = compilerArguments?.let {
      CompilerArgumentsSerializer.serializeToString(it)
    } ?: ""
    this.compilerSettings =
      CompilerSettingsData(
        additionalArguments = "",  // We already set arguments via this.compilerArguments. Don't duplicate to avoid red code
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

  private fun MutableEntityStorage.addKotlinSettingsEntity(
    parentModuleEntity: ModuleEntity,
    kotlinSettingsEntity: KotlinSettingsEntityBuilder,
  ): KotlinSettingsEntity {
    val updatedParentModuleEntity =
      modifyModuleEntity(parentModuleEntity) {
        this.kotlinSettings += kotlinSettingsEntity
      }

    return updatedParentModuleEntity.kotlinSettings.last()
  }
}
