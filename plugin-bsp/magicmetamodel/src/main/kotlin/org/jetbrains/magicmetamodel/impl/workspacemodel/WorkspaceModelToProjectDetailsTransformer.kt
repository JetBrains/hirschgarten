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
import com.intellij.java.workspace.entities.JavaResourceRootPropertiesEntity
import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.serialization.impl.toPath
import com.intellij.util.io.isDirectory
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.LoadedTargetsStorage
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleCapabilities

public object WorkspaceModelToProjectDetailsTransformer {
  public operator fun invoke(workspaceModel: WorkspaceModel,
                             loadedTargetsStorage: LoadedTargetsStorage,
                             moduleNameProvider: ModuleNameProvider): ProjectDetails =
    with(workspaceModel.currentSnapshot) {
      EntitiesToProjectDetailsTransformer(
        entities(ModuleEntity::class.java),
        entities(SourceRootEntity::class.java),
        entities(LibraryEntity::class.java),
        loadedTargetsStorage.getLoadedTargets().associateBy { id -> moduleNameProvider(id) },
      )
    }

  internal object EntitiesToProjectDetailsTransformer {

    private data class ModuleParsingData(
      val target: BuildTarget,
      val sourcesItem: SourcesItem?,
      val resourcesItem: ResourcesItem?,
      val libSources: DependencySourcesItem?,
      val libJars: JavacOptionsItem?,
      val outputPathUris: List<String>,
    )

    operator fun invoke(
      loadedModules: Sequence<ModuleEntity>,
      sourceRoots: Sequence<SourceRootEntity>,
      libraries: Sequence<LibraryEntity>,
      loadedTargetsIndex: Map<String, BuildTargetIdentifier>,
    ): ProjectDetails {
      val librariesIndex = libraries.associateBy { it.symbolicId }
      val modulesWithSources = sourceRoots.groupBy { it.contentRoot.module }
      val modulesWithoutSources =
        loadedModules
          .mapNotNull { it.takeUnless { modulesWithSources.contains(it) }
            ?.to(emptyList<SourceRootEntity>()) }
      val allModules = modulesWithSources + modulesWithoutSources
      val modulesParsingData = allModules.mapNotNull {
        it.toModuleParsingData(librariesIndex, loadedTargetsIndex)
      }
      val targets = modulesParsingData.map { it.target }
      return ProjectDetails(
        targetsId = targets.map { it.id },
        targets = targets.toSet(),
        sources = modulesParsingData.mapNotNull { it.sourcesItem },
        resources = modulesParsingData.mapNotNull { it.resourcesItem },
        dependenciesSources = modulesParsingData.mapNotNull { it.libSources },
        javacOptions = modulesParsingData.mapNotNull { it.libJars },
        outputPathUris = modulesParsingData.flatMap { it.outputPathUris },
      )
    }

    private fun Map.Entry<ModuleEntity, List<SourceRootEntity>>.toModuleParsingData(
      libraries: Map<LibraryId, LibraryEntity>,
      loadedTargetsIndex: Map<String, BuildTargetIdentifier>
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
      val outputPathUris = module.toOutputPathUris()
      return ModuleParsingData(
        target = target,
        sourcesItem = sourcesItem,
        resourcesItem = resourcesItem,
        libSources = dependencySourcesItem,
        libJars = javacSourcesItem,
        outputPathUris = outputPathUris,
      )
    }
  }

  internal object JavaSourceRootToSourceItemTransformer {

    operator fun invoke(entity: JavaSourceRootPropertiesEntity): SourceItem {
      val path = entity.sourceRoot.url.toPath()
      val kind = if (path.isDirectory()) SourceItemKind.DIRECTORY else SourceItemKind.FILE
      return SourceItem(path.toUri().toString(), kind, entity.generated)
    }
  }

  private fun List<ModuleDependencyItem>.toBuildTargetIdentifiers(
    loadedTargetsIndex: Map<String, BuildTargetIdentifier>
  ) =
    filterIsInstance<ModuleDependencyItem.Exportable.ModuleDependency>()
      .map { loadedTargetsIndex[it.module.name] }

  private fun JavaResourceRootPropertiesEntity.toResourcePath() = sourceRoot.url.toPath().toUri().toString()

  private fun SourceRootEntity.toSourceItems() =
    javaSourceRoots
      .map { JavaSourceRootToSourceItemTransformer.invoke(it) }

  private fun SourceRootEntity.toResourcePaths() = javaResourceRoots.map { it.toResourcePath() }

  private fun Map<LibraryId, LibraryEntity>.getLibrariesForModule(module: ModuleEntity): Sequence<LibraryEntity> =
    module.dependencies.filterIsInstance<ModuleDependencyItem.Exportable.LibraryDependency>()
      .mapNotNull { this[it.library] }
      .asSequence()

  private fun Sequence<LibraryEntity>.filterRoots(type: LibraryRootTypeId) =
    flatMap { lib ->
      lib.roots.filter { it.type == type }.map { it.url.toPath().toUri().toString() }
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
        it.baseDirectory = baseDirContentRoot?.url?.toPath()?.toUri()?.toString()
        sdkDependency?.addToBuildTarget(it)
      }
    }

  private fun ModuleDependencyItem.SdkDependency.addToBuildTarget(target: BuildTarget) {
    target.dataKind = BuildTargetDataKind.JVM
    target.data = Gson().toJson(JvmBuildTarget("", sdkName))
  }

  private fun ModuleEntity.toOutputPathUris(): List<String> =
    contentRoots
      .flatMap { it.excludedUrls }
      .map { it.url.toPath().toUri().toString() }
}
