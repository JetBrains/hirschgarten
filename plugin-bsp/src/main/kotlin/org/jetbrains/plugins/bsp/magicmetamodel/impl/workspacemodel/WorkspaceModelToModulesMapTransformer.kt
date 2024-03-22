package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel

import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.java.workspace.entities.javaSettings
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.customImlData
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.bsp.protocol.AndroidTargetType
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.idea.workspaceModel.CompilerArgumentsSerializer
import org.jetbrains.kotlin.idea.workspaceModel.KotlinSettingsEntity
import org.jetbrains.kotlin.idea.workspaceModel.kotlinSettings
import org.jetbrains.plugins.bsp.magicmetamodel.ModuleNameProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.TargetsStatusStorage
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.PythonSdkInfo.Companion.PYTHON_SDK_ID
import org.jetbrains.workspacemodel.entities.AndroidTargetType.APP
import org.jetbrains.workspacemodel.entities.AndroidTargetType.LIBRARY
import org.jetbrains.workspacemodel.entities.AndroidTargetType.TEST
import org.jetbrains.workspacemodel.entities.androidAddendumEntity
import org.jetbrains.workspacemodel.entities.jvmBinaryJarsEntity
import java.nio.file.Path

public object WorkspaceModelToModulesMapTransformer {
  public operator fun invoke(
    workspaceModel: WorkspaceModel,
    targetsStatusStorage: TargetsStatusStorage,
    targetsMap: Map<BuildTargetId, BuildTargetInfo>,
    moduleNameProvider: ModuleNameProvider,
  ): Map<BuildTargetId, Module> =
    with(workspaceModel.currentSnapshot) {
      WorkspaceModelToMagicMetamodelTransformer(
        entities(ModuleEntity::class.java),
        entities(SourceRootEntity::class.java),
        entities(LibraryEntity::class.java),
        targetsStatusStorage.getLoadedTargets().mapNotNull { targetsMap[it] }.associateBy(
          keySelector = { it.id },
          valueTransform = { moduleNameProvider(it) }
        ),
      )
    }

  internal object WorkspaceModelToMagicMetamodelTransformer {
    operator fun invoke(
      loadedModules: Sequence<ModuleEntity>,
      sourceRoots: Sequence<SourceRootEntity>,
      libraries: Sequence<LibraryEntity>,
      loadedTargetsIndex: Map<String, BuildTargetId>,
    ): Map<BuildTargetId, Module> {
      val librariesIndex = libraries.map { entity ->
        entity.symbolicId to Library(
          displayName = entity.name,
          sourceJars = entity.librarySources(),
          classJars = entity.libraryClasses(),
        )
      }.toMap()
      val modulesWithSources = sourceRoots.groupBy { it.contentRoot.module }
      val modulesWithoutSources =
        loadedModules
          .mapNotNull {
            it.takeUnless { modulesWithSources.contains(it) }
              ?.to(emptyList<SourceRootEntity>())
          }
      val allModules = modulesWithSources + modulesWithoutSources
      val modulesParsingData = allModules.mapNotNull {
        val moduleLibraries = librariesIndex.librariesForModule(it.key)
        it.parseModules(moduleLibraries, loadedTargetsIndex)
      }
      return modulesParsingData.toMap()
    }
  }

  private fun Map.Entry<ModuleEntity, List<SourceRootEntity>>.parseModules(
    libraries: Sequence<Library>,
    loadedTargetsIndex: Map<String, BuildTargetId>,
  ): Pair<BuildTargetId, Module>? {
    val (entity, sources) = this
    return loadedTargetsIndex[entity.name]?.let { moduleName ->
      val genericModuleInfo = GenericModuleInfo(
        name = entity.name,
        type = entity.type.orEmpty(),
        modulesDependencies = entity.dependencies.filterModuleDependencies(),
        librariesDependencies = entity.dependencies.filterLibraryDependencies(),
        capabilities = ModuleCapabilities(entity.customImlData?.customModuleOptions.orEmpty()),
        languageIds = entity.customImlData?.customModuleOptions.orEmpty(),
        associates = entity.getKotlinSettings()?.getAssociates().orEmpty(),
      )
      val module = if (genericModuleInfo.languageIds.includesPython()) {
        PythonModule(
          module = genericModuleInfo,
          sourceRoots = sources.map { it.toGenericSourceRoot() },
          resourceRoots = sources.map { it.toResourceRoot() }.flatten(),
          libraries = libraries.map { PythonLibrary(it.sourceJars) }.toList(),
          sdkInfo = entity.getPythonSdk(),
        )
      } else {
        val baseDirContentRoot = entity.getBaseDir()
        JavaModule(
          genericModuleInfo = genericModuleInfo,
          baseDirContentRoot = baseDirContentRoot,
          sourceRoots = sources.map { it.toJavaSourceRoot() }.flatten(),
          resourceRoots = sources.map { it.toResourceRoot() }.flatten(),
          moduleLevelLibraries = libraries.toList(),
          jvmJdkName = entity.getSdkName(),
          jvmBinaryJars = entity.getJvmBinaryJars(),
          kotlinAddendum = entity.getKotlinSettings()?.toKotlinAddendum(),
          javaAddendum = entity.toJavaAddendum(),
          androidAddendum = entity.toAndroidAddendum(),
        )
      }
      moduleName to module
    }
  }

  private fun ModuleEntity.getKotlinSettings() =
    kotlinSettings.firstOrNull()

  private fun KotlinSettingsEntity.getAssociates() =
    additionalVisibleModuleNames.map { IntermediateModuleDependency(it) }

  private fun KotlinSettingsEntity.toKotlinAddendum(): KotlinAddendum? {
    val deserializedCompilerArgs =
      CompilerArgumentsSerializer.deserializeFromString(compilerArguments) as? K2JVMCompilerArguments ?: return null
    val languageVersion = deserializedCompilerArgs.languageVersion ?: return null
    val apiVersion = deserializedCompilerArgs.apiVersion ?: return null
    return KotlinAddendum(
      languageVersion = languageVersion,
      apiVersion = apiVersion,
      kotlincOptions = deserializedCompilerArgs.toArgumentStrings(),
    )
  }

  private fun ModuleEntity.toJavaAddendum() = javaSettings?.languageLevelId?.let { JavaAddendum(it) }

  private fun ModuleEntity.toAndroidAddendum(): AndroidAddendum? {
    val androidAddendumEntity = this.androidAddendumEntity ?: return null

    val androidTargetType = when (androidAddendumEntity.androidTargetType) {
      APP -> AndroidTargetType.APP
      LIBRARY -> AndroidTargetType.LIBRARY
      TEST -> AndroidTargetType.TEST
    }

    return with(androidAddendumEntity) {
      AndroidAddendum(
        androidSdkName = androidSdkName,
        androidTargetType = androidTargetType,
        manifest = manifest?.toPath(),
        resourceFolders = resourceFolders.map { it.toPath() },
      )
    }
  }

  private fun ModuleEntity.getBaseDir() =
    // TODO: in `ModuleDetailsToJavaModuleTransformer` we are assuming there will be one or we are providing a fake one
    contentRoots.firstOrNull { it.sourceRoots.isEmpty() }?.let { entity ->
      ContentRoot(
        entity.url.toPath(),
        entity.excludedUrls.map { it.url.toPath() },
      )
    }

  private fun List<ModuleDependencyItem>.filterModuleDependencies() =
    filterIsInstance<ModuleDependency>()
      .map { IntermediateModuleDependency(it.module.name) }

  private fun List<ModuleDependencyItem>.filterLibraryDependencies() =
    filterIsInstance<LibraryDependency>()
      .map { IntermediateLibraryDependency(it.library.name) }

  private fun SourceRootEntity.toJavaSourceRoot() = javaSourceRoots.map { entity ->
    JavaSourceRoot(
      sourcePath = entity.sourceRoot.contentRoot.url.toPath(),
      generated = entity.generated,
      packagePrefix = entity.packagePrefix,
      rootType = entity.sourceRoot.rootType,
      excludedPaths = entity.sourceRoot.contentRoot.excludedUrls.map { it.url.toPath() },
    )
  }

  private fun SourceRootEntity.toGenericSourceRoot() = GenericSourceRoot(
    sourcePath = contentRoot.url.toPath(),
    rootType = rootType,
    excludedPaths = contentRoot.excludedUrls.map { it.url.toPath() },
  )
}

private fun SourceRootEntity.toResourceRoot() = javaResourceRoots.map { entity ->
  ResourceRoot(
    resourcePath = entity.sourceRoot.contentRoot.url.toPath(),
    rootType = entity.sourceRoot.rootType,
  )
}

private fun Map<LibraryId, Library>.librariesForModule(module: ModuleEntity): Sequence<Library> =
  module.dependencies.filterIsInstance<LibraryDependency>()
    .mapNotNull { this[it.library] }
    .asSequence()

private fun ModuleEntity.getSdkName(): String? =
  dependencies.filterIsInstance<SdkDependency>().firstOrNull()?.sdk?.name

private fun ModuleEntity.getJvmBinaryJars(): List<Path> =
  jvmBinaryJarsEntity?.jars?.map { it.toPath() }.orEmpty()

private fun ModuleEntity.getPythonSdk(): PythonSdkInfo? =
  dependencies.filterIsInstance<SdkDependency>()
    .firstOrNull { it.sdk.type == PYTHON_SDK_ID }
    ?.let { PythonSdkInfo.fromString(it.sdk.name) }

private fun LibraryEntity.filterRoots(type: LibraryRootTypeId) =
  roots.filter { it.type == type }.map { it.url.url }

private fun LibraryEntity.librarySources() =
  filterRoots(LibraryRootTypeId.SOURCES)

private fun LibraryEntity.libraryClasses() =
  filterRoots(LibraryRootTypeId.COMPILED)
