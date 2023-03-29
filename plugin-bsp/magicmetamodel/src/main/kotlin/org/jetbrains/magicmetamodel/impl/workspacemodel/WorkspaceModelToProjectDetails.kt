package org.jetbrains.magicmetamodel.impl.workspacemodel


import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import com.google.gson.Gson
import com.intellij.util.io.isDirectory
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.storage.bridgeEntities.JavaResourceRootPropertiesEntity
import com.intellij.workspaceModel.storage.bridgeEntities.JavaSourceRootPropertiesEntity
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryEntity
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryId
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRootTypeId
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleDependencyItem
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.LoadedTargetsStorage
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleCapabilities
import kotlin.io.path.Path

public object WorkspaceModelToProjectDetailsTransformer {
  public operator fun invoke(workspaceModel: WorkspaceModel,
                             loadedTargetsStorage: LoadedTargetsStorage,
                             moduleNameProvider: ((BuildTargetIdentifier) -> String)?): ProjectDetails = with(workspaceModel.currentSnapshot) {
    EntitiesToProjectDetailsTransformer(
      entities(ModuleEntity::class.java),
      entities(SourceRootEntity::class.java),
      entities(LibraryEntity::class.java),
      loadedTargetsStorage,
      moduleNameProvider,
    )
  }

  internal object EntitiesToProjectDetailsTransformer {

    private lateinit var loadedTargetsIndex: Map<String, BuildTargetIdentifier>

    private data class ModuleParsingData(
      val target: BuildTarget,
      val sourcesItem: SourcesItem?,
      val resourcesItem: ResourcesItem?,
      val libSources: DependencySourcesItem?,
      val libJars: JavacOptionsItem?,
    )

    operator fun invoke(
      loadedModules: Sequence<ModuleEntity>,
      sourceRoots: Sequence<SourceRootEntity>,
      libraries: Sequence<LibraryEntity>,
      loadedTargetsStorage: LoadedTargetsStorage,
      moduleNameProvider: ((BuildTargetIdentifier) -> String)? = null
    ): ProjectDetails {
      loadedTargetsIndex = loadedTargetsStorage.getLoadedTargets().associateBy { id -> (moduleNameProvider?.let { it(id) }) ?: id.uri }

      val librariesIndex = libraries.associateBy { it.symbolicId }
      val modulesWithSources = sourceRoots.groupBy { it.contentRoot.module }
      val modulesWithoutSources =
        loadedModules.mapNotNull { it.takeUnless(modulesWithSources::contains)?.to(emptyList<SourceRootEntity>()) }
      val allModules = modulesWithSources + modulesWithoutSources
      val modulesParsingData = allModules.mapNotNull {
        it.toModuleParsingData(librariesIndex)
      }
      val targets = modulesParsingData.map(ModuleParsingData::target)
      return ProjectDetails(
        targetsId = targets.map(BuildTarget::getId),
        targets = targets.toSet(),
        sources = modulesParsingData.mapNotNull(ModuleParsingData::sourcesItem),
        resources = modulesParsingData.mapNotNull(ModuleParsingData::resourcesItem),
        dependenciesSources = modulesParsingData.mapNotNull(ModuleParsingData::libSources),
        javacOptions = modulesParsingData.mapNotNull(ModuleParsingData::libJars),
      )
    }

    private fun Map.Entry<ModuleEntity, List<SourceRootEntity>>.toModuleParsingData(
      libraries: Map<LibraryId, LibraryEntity>,
    ): ModuleParsingData? {
      val (module, sources) = this
      val target = module.toBuildTarget(loadedTargetsIndex) ?: return null

      val moduleLibraries = libraries.getLibrariesForModule(module)
      val dependencySourcesItem = moduleLibraries.librariesSources(target.id)
      val javacSourcesItem = moduleLibraries.librariesJars(target.id)
      var sourcesItem: SourcesItem? = null
      var resourcesItem: ResourcesItem? = null
      if (sources.isNotEmpty()) {
        sourcesItem = SourcesItem(target.id, sources.flatMap { it.toSourceItems() })
        resourcesItem = ResourcesItem(target.id, sources.flatMap { it.toResourcePaths() })
      }
      return ModuleParsingData(
        target = target,
        sourcesItem = sourcesItem,
        resourcesItem = resourcesItem,
        libSources = dependencySourcesItem,
        libJars = javacSourcesItem,
      )
    }
  }

  internal object JavaSourceRootToSourceItemTransformer {

    operator fun invoke(entity: JavaSourceRootPropertiesEntity): SourceItem {
      val path = entity.sourceRoot.url.presentableUrl
      val kind = if (Path(path).isDirectory()) SourceItemKind.DIRECTORY else SourceItemKind.FILE
      return SourceItem(path, kind, entity.generated)
    }
  }

  private fun List<ModuleDependencyItem>.toBuildTargetIdentifiers(loadedTargetsIndex: Map<String, BuildTargetIdentifier>) =
    filterIsInstance<ModuleDependencyItem.Exportable.ModuleDependency>().map { loadedTargetsIndex[it.module.name] }

  private fun JavaResourceRootPropertiesEntity.toResourcePath() = sourceRoot.url.presentableUrl

  private fun SourceRootEntity.toSourceItems() = javaSourceRoots.map(JavaSourceRootToSourceItemTransformer::invoke)

  private fun SourceRootEntity.toResourcePaths() = javaResourceRoots.map { it.toResourcePath() }

  private fun Map<LibraryId, LibraryEntity>.getLibrariesForModule(module: ModuleEntity): Sequence<LibraryEntity> =
    module.dependencies.filterIsInstance<ModuleDependencyItem.Exportable.LibraryDependency>()
      .mapNotNull { this[it.library] }.asSequence()

  private fun Sequence<LibraryEntity>.filterRoots(type: LibraryRootTypeId) =
    flatMap { lib ->
      lib.roots.filter { it.type == type }.map { it.url.presentableUrl }
    }.takeIf {
      it.iterator().hasNext()
    }

  private fun Sequence<LibraryEntity>.librariesSources(target: BuildTargetIdentifier) =
    filterRoots(LibraryRootTypeId.SOURCES)?.let {
      DependencySourcesItem(target, it.toList())
    }

  private fun Sequence<LibraryEntity>.librariesJars(target: BuildTargetIdentifier) =
    filterRoots(LibraryRootTypeId.COMPILED)?.let {
      JavacOptionsItem(target, emptyList(), it.toList(), "")
    }

  private fun ModuleEntity.toBuildTarget(loadedTargetsIndex: Map<String, BuildTargetIdentifier>): BuildTarget? =
    loadedTargetsIndex[name]?.let { moduleName ->
      val sdkDependency = this.dependencies.filterIsInstance<ModuleDependencyItem.SdkDependency>().firstOrNull()
      // TODO prob we can remove it, but we dont want to do it a few hours before the release
      val baseDirContentRoot = contentRoots.firstOrNull { it.sourceRoots.isEmpty() }
      val capabilities = ModuleCapabilities(customImlData?.customModuleOptions.orEmpty())
      val modulesDeps = dependencies.toBuildTargetIdentifiers(loadedTargetsIndex)
      return BuildTarget(
        moduleName, emptyList(), emptyList(), modulesDeps, BuildTargetCapabilities(
          capabilities.canCompile, capabilities.canTest, capabilities.canRun, capabilities.canDebug
        )
      ).also {
        it.baseDirectory = baseDirContentRoot?.url?.presentableUrl
        sdkDependency?.addToBuildTarget(it)
      }
    }

  private fun ModuleDependencyItem.SdkDependency.addToBuildTarget(target: BuildTarget) {
    target.dataKind = BuildTargetDataKind.JVM
    target.data = Gson().toJson(JvmBuildTarget("", sdkName))
  }
}



