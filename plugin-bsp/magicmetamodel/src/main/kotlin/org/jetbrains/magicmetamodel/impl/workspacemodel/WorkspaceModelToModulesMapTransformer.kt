package org.jetbrains.magicmetamodel.impl.workspacemodel

import com.intellij.facet.impl.FacetUtil
import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.java.workspace.entities.javaSettings
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.openapi.util.JDOMUtil
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.kotlin.idea.facet.KotlinFacetConfiguration
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.LoadedTargetsStorage
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonSdkInfo.Companion.PYTHON_SDK_ID
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.toKotlinCOption

public object WorkspaceModelToModulesMapTransformer {
  public operator fun invoke(
    workspaceModel: WorkspaceModel,
    loadedTargetsStorage: LoadedTargetsStorage,
    moduleNameProvider: ModuleNameProvider,
  ): Map<BuildTargetId, Module> =
    with(workspaceModel.currentSnapshot) {
      WorkspaceModelToMagicMetamodelTransformer(
        entities(ModuleEntity::class.java),
        entities(SourceRootEntity::class.java),
        entities(LibraryEntity::class.java),
        loadedTargetsStorage.getLoadedTargets().associateBy { id -> moduleNameProvider(id) },
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
      val kotlinConfiguration = entity.getKotlinConfiguration()
      val genericModuleInfo = GenericModuleInfo(
        name = entity.name,
        type = entity.type.orEmpty(),
        modulesDependencies = entity.dependencies.filterModuleDependencies(),
        librariesDependencies = entity.dependencies.filterLibraryDependencies(),
        capabilities = ModuleCapabilities(entity.customImlData?.customModuleOptions.orEmpty()),
        languageIds = entity.customImlData?.customModuleOptions.orEmpty(),
        associates = kotlinConfiguration?.getAssociates().orEmpty(),
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
          compilerOutput = entity.javaSettings?.compilerOutput?.toPath(),
          moduleLevelLibraries = libraries.toList(),
          jvmJdkName = entity.getSdkName(),
          kotlinAddendum = kotlinConfiguration?.toKotlinAddendum(),
        )
      }
      moduleName to module
    }
  }

  private fun ModuleEntity.getKotlinConfiguration() =
    facets.firstOrNull { it.facetType == KotlinFacetType.INSTANCE.id.toString() }
      ?.configurationXmlTag
      ?.let { createKotlinConfiguration(it) }

  private fun createKotlinConfiguration(configuration: String) =
    KotlinFacetType.INSTANCE.createDefaultConfiguration().apply {
      val element = JDOMUtil.load(configuration)
      FacetUtil.loadFacetConfiguration(this, element)
    }

  private fun KotlinFacetConfiguration.getAssociates() =
    settings.additionalVisibleModuleNames.map { ModuleDependency(it) }

  private fun KotlinFacetConfiguration.toKotlinAddendum() =
    KotlinAddendum(
      languageVersion = settings.languageLevel?.versionString.orEmpty(),
      apiVersion = settings.apiLevel?.toString().orEmpty(),
      kotlincOptions = settings.compilerArguments?.toKotlinCOption(),
    )

  private fun ModuleEntity.getBaseDir() =
    // TODO: in `ModuleDetailsToJavaModuleTransformer` we are assuming there will be one or we are providing a fake one
    contentRoots.firstOrNull { it.sourceRoots.isEmpty() }?.let { entity ->
      ContentRoot(
        entity.url.toPath(),
        entity.excludedUrls.map { it.url.toPath() },
      )
    }

  private fun List<ModuleDependencyItem>.filterModuleDependencies() =
    filterIsInstance<ModuleDependencyItem.Exportable.ModuleDependency>()
      .map { ModuleDependency(it.module.name) }

  private fun List<ModuleDependencyItem>.filterLibraryDependencies() =
    filterIsInstance<ModuleDependencyItem.Exportable.LibraryDependency>()
      .map { LibraryDependency(it.library.name) }

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
    entity.sourceRoot.contentRoot.url.toPath(),
  )
}

private fun Map<LibraryId, Library>.librariesForModule(module: ModuleEntity): Sequence<Library> =
  module.dependencies.filterIsInstance<ModuleDependencyItem.Exportable.LibraryDependency>()
    .mapNotNull { this[it.library] }
    .asSequence()

private fun ModuleEntity.getSdkName(): String? =
  dependencies.filterIsInstance<ModuleDependencyItem.SdkDependency>().firstOrNull()?.sdkName

private fun ModuleEntity.getPythonSdk(): PythonSdkInfo? =
  dependencies.filterIsInstance<ModuleDependencyItem.SdkDependency>()
    .firstOrNull { it.sdkType == PYTHON_SDK_ID }
    ?.let { PythonSdkInfo.fromString(it.sdkName) }

private fun LibraryEntity.filterRoots(type: LibraryRootTypeId) =
  roots.filter { it.type == type }.map { it.url.url }

private fun LibraryEntity.librarySources() =
  filterRoots(LibraryRootTypeId.SOURCES)

private fun LibraryEntity.libraryClasses() =
  filterRoots(LibraryRootTypeId.COMPILED)
