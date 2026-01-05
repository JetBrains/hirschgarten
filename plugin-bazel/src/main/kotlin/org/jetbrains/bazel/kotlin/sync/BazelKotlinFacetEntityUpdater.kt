package org.jetbrains.bazel.kotlin.sync

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.jpsCompilation.utils.JpsPaths
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.KotlinFacetEntityUpdater
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.KotlinAddendum
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
import java.nio.file.Path

class BazelKotlinFacetEntityUpdater : KotlinFacetEntityUpdater {
  override fun addEntity(
    diff: MutableEntityStorage,
    entityToAdd: JavaModule,
    parentModuleEntity: ModuleEntity,
    projectBasePath: Path,
  ) {
    val kotlinAddendum = entityToAdd.kotlinAddendum
    val compilerArguments = kotlinAddendum?.kotlincOptions?.toK2JVMCompilerArguments(entityToAdd, kotlinAddendum, projectBasePath)
    val kotlinSettingsEntity =
      calculateKotlinSettingsEntity(entityToAdd, compilerArguments, parentModuleEntity, kotlinAddendum?.kotlincOptions)
    diff.addKotlinSettingsEntity(parentModuleEntity, kotlinSettingsEntity)
  }

  private fun List<String>.toK2JVMCompilerArguments(
    entityToAdd: JavaModule,
    kotlinAddendum: KotlinAddendum,
    projectBasePath: Path,
  ) = parseCommandLineArguments(K2JVMCompilerArguments::class, this).apply {
    languageVersion = kotlinAddendum.languageVersion
    apiVersion = kotlinAddendum.apiVersion
    moduleName = kotlinAddendum.moduleName

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
    moduleId = parentModuleEntity.symbolicId,
    name = KotlinFacetType.NAME,
    sourceRoots = emptyList(),
    configFileItems = emptyList(),
    useProjectSettings = false,
    implementedModuleNames = emptyList(),
    dependsOnModuleNames = emptyList(), // Gradle specific
    additionalVisibleModuleNames = entityToAdd.toAssociateModules().toMutableSet(),
    sourceSetNames = emptyList(),
    isTestModule = entityToAdd.genericModuleInfo.kind.ruleType == RuleType.TEST,
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
    this.genericModuleInfo.associates
      .toSet()

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
