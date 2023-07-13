package org.jetbrains.magicmetamodel.impl

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
import org.jetbrains.annotations.Nullable
import org.jetbrains.magicmetamodel.LibraryItem
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.KotlinBuildTarget

// TODO, we can do it better, but for now it should be good enough:
// the pros (in my opinion @abrams27) of this solution:
// - it's easier to implement than some custom serialization / deserialization
// - the idea is simple - .toState() and .fromState()
// - the real entities and the state entities are separate
// - it works
// theoretically we could use those instead of real BSP entities,
// but those are mutable (well, BSP are mutable as well)


public interface ConvertableFromState<out T> {
  public fun fromState(): T
}

public interface ConvertableToState<out T> {
  public fun toState(): T
}


public data class BuildTargetIdentifierState(
  public var uri: String = ""
) {

  public fun fromState(): BuildTargetIdentifier =
    BuildTargetIdentifier(uri)
}

public fun BuildTargetIdentifier.toState(): BuildTargetIdentifierState =
  BuildTargetIdentifierState(uri)

public fun LibraryItem.toState(): LibraryItemState =
        LibraryItemState(
                id = this.id.toState(),
                dependencies = this.dependencies.map { it.toState() },
                uris = this.jars
        )


public data class BuildTargetCapabilitiesState(
  public var canCompile: Boolean = false,
  public var canTest: Boolean = false,
  public var canRun: Boolean = false,
  public var canDebug: Boolean = false
) : ConvertableFromState<BuildTargetCapabilities> {

  public override fun fromState(): BuildTargetCapabilities =
    BuildTargetCapabilities(canCompile, canTest, canRun, canDebug)
}

public fun BuildTargetCapabilities.toState(): BuildTargetCapabilitiesState =
  BuildTargetCapabilitiesState(
    canCompile = canCompile,
    canTest = canTest,
    canRun = canRun,
    canDebug = canDebug,
  )

public data class LibraryItemState(
        public var id: BuildTargetIdentifierState = BuildTargetIdentifierState(),
        public var dependencies: List<BuildTargetIdentifierState> = emptyList(),
        public var uris: List<String> = emptyList(),
) : ConvertableFromState<LibraryItem> {

  public override fun fromState(): LibraryItem =
          LibraryItem(
                  id.fromState(),
                  dependencies.map { it.fromState() },
                  emptyList()
          )
}

public data class BuildTargetState(
  public var id: BuildTargetIdentifierState = BuildTargetIdentifierState(),
  public var displayName: String? = null,
  public var baseDirectory: String? = null,
  public var tags: List<String> = emptyList(),
  public var languageIds: List<String> = emptyList(),
  public var dependencies: List<BuildTargetIdentifierState> = emptyList(),
  public var capabilities: BuildTargetCapabilitiesState = BuildTargetCapabilitiesState(),
  public var dataKind: String? = null,
  public var data: String? = null,
) : ConvertableFromState<BuildTarget> {

  public override fun fromState(): BuildTarget =
    BuildTarget(
      id.fromState(),
      tags,
      languageIds,
      dependencies.map { it.fromState() },
      capabilities.fromState()
    ).apply {
      displayName = this@BuildTargetState.displayName
      baseDirectory = this@BuildTargetState.baseDirectory
      dataKind = this@BuildTargetState.dataKind
      data = when(this@BuildTargetState.dataKind) {
        BuildTargetDataKind.JVM -> Gson().fromJson(this@BuildTargetState.data, JvmBuildTarget::class.java)
        "kotlin" -> Gson().fromJson(this@BuildTargetState.data, KotlinBuildTarget::class.java)
        else -> null
      }
    }
}

public fun BuildTarget.toState(): BuildTargetState =
  BuildTargetState(
    id = id.toState(),
    displayName = displayName,
    baseDirectory = baseDirectory,
    tags = tags,
    languageIds = languageIds,
    dependencies = dependencies.map { it.toState() },
    capabilities = capabilities.toState(),
    dataKind = dataKind,
    data = Gson().toJson(data),
  )


public data class SourceItemState(
  public var uri: String = "",
  public var kind: Int = 0,
  public var generated: Boolean = false,
) : ConvertableFromState<SourceItem> {

  public override fun fromState(): SourceItem =
    SourceItem(uri, SourceItemKind.forValue(kind), generated)
}

public fun SourceItem.toState(): SourceItemState =
  SourceItemState(
    uri = uri,
    kind = kind.value,
    generated = generated,
  )


public data class SourcesItemState(
  public var target: BuildTargetIdentifierState = BuildTargetIdentifierState(),
  public var sources: List<SourceItemState> = emptyList(),
  public var roots: List<String>? = null,
) : ConvertableFromState<SourcesItem> {

  public override fun fromState(): SourcesItem =
    SourcesItem(target.fromState(), sources.map { it.fromState() }).apply {
      roots = this@SourcesItemState.roots
    }
}

public fun SourcesItem.toState(): SourcesItemState =
  SourcesItemState(
    target = target.toState(),
    sources = sources.map { it.toState() },
    roots = roots,
  )


public class ResourcesItemState(
  public var target: BuildTargetIdentifierState = BuildTargetIdentifierState(),
  public var resources: List<String> = emptyList(),
) : ConvertableFromState<ResourcesItem> {

  public override fun fromState(): ResourcesItem =
    ResourcesItem(target.fromState(), resources)
}

public fun ResourcesItem.toState(): ResourcesItemState =
  ResourcesItemState(
    target = target.toState(),
    resources = resources,
  )


public class DependencySourcesItemState(
  public var target: BuildTargetIdentifierState = BuildTargetIdentifierState(),
  public var sources: List<String> = emptyList(),
) : ConvertableFromState<DependencySourcesItem> {

  public override fun fromState(): DependencySourcesItem =
    DependencySourcesItem(target.fromState(), sources)
}

public fun DependencySourcesItem.toState(): DependencySourcesItemState =
    DependencySourcesItemState(
      target = target.toState(),
      sources = sources,
    )


public class JavacOptionsItemState(
  public var target: BuildTargetIdentifierState = BuildTargetIdentifierState(),
  public var options: List<String> = emptyList(),
  public var classpath: List<String> = emptyList(),
  public var classDirectory: String = ""
) : ConvertableFromState<JavacOptionsItem> {

  public override fun fromState(): JavacOptionsItem =
    JavacOptionsItem(target.fromState(), options, classpath, classDirectory)
}

public fun JavacOptionsItem.toState(): JavacOptionsItemState =
  JavacOptionsItemState(
    target = target.toState(),
    options= options,
    classpath = classpath,
    classDirectory = classDirectory,
  )

public data class ProjectDetailsState(
  public var targetsId: List<BuildTargetIdentifierState> = emptyList(),
  public var targets: List<BuildTargetState> = emptyList(),
  public var sources: List<SourcesItemState> = emptyList(),
  public var resources: List<ResourcesItemState> = emptyList(),
  public var dependenciesSources: List<DependencySourcesItemState> = emptyList(),
  public var javacOptions: List<JavacOptionsItemState> = emptyList(),
  public var outputPathUris: List<String> = emptyList(),
  @Nullable public var libraries: List<LibraryItemState>? = null,
  ) : ConvertableFromState<ProjectDetails> {

  public override fun fromState(): ProjectDetails =
    ProjectDetails(
      targetsId = targetsId.map { it.fromState() },
      targets = targets.map { it.fromState() }.toSet(),
      sources = sources.map { it.fromState() },
      resources = resources.map { it.fromState() },
      dependenciesSources = dependenciesSources.map { it.fromState() },
      javacOptions = javacOptions.map { it.fromState() },
      outputPathUris = outputPathUris,
      libraries = libraries?.map { it.fromState() },
    )
}

public fun ProjectDetails.toState(): ProjectDetailsState =
  ProjectDetailsState(
    targetsId = targetsId.map { it.toState() },
    targets = targets.map { it.toState() },
    sources = sources.map { it.toState() },
    resources = resources.map { it.toState() },
    dependenciesSources = dependenciesSources.map { it.toState() },
    javacOptions = javacOptions.map { it.toState() },
    libraries = libraries?.map { it.toState() }
  )

public fun ProjectDetails.toStateWithoutLoadedTargets(loaded: List<BuildTargetIdentifier>): ProjectDetailsState =
  ProjectDetailsState(
    targetsId = targetsId.filterNot { loaded.contains(it) }.map { it.toState() },
    targets = targets.filterNot { loaded.contains(it.id) }.map { it.toState() },
    sources = sources.filterNot { loaded.contains(it.target) }.map { it.toState() },
    resources = resources.filterNot { loaded.contains(it.target) }.map { it.toState() },
    dependenciesSources = dependenciesSources.filterNot { loaded.contains(it.target) }.map { it.toState() },
    javacOptions = javacOptions.filterNot { loaded.contains(it.target) }.map { it.toState() },
    libraries = libraries?.map { it.toState() }
  )


public data class ModuleDetailsState(
  public var target: BuildTargetState = BuildTargetState(),
  public var allTargetsIds: List<BuildTargetIdentifierState> = emptyList(),
  public var sources: List<SourcesItemState> = emptyList(),
  public var resources: List<ResourcesItemState> = emptyList(),
  public var dependenciesSources: List<DependencySourcesItemState> = emptyList(),
  public var javacOptions: JavacOptionsItemState? = null,
  public var outputPathUris: List<String> = emptyList(),
  public var libraryDependencies: List<BuildTargetIdentifierState>? = emptyList(),
  public var moduleDependencies: List<BuildTargetIdentifierState> = emptyList(),
  ) : ConvertableFromState<ModuleDetails> {

  public override fun fromState(): ModuleDetails =
    ModuleDetails(
      target = target.fromState(),
      allTargetsIds = allTargetsIds.map { it.fromState() },
      sources = sources.map { it.fromState() },
      resources = resources.map { it.fromState() },
      dependenciesSources = dependenciesSources.map { it.fromState() },
      javacOptions = javacOptions?.fromState(),
      outputPathUris = outputPathUris,
      libraryDependencies = libraryDependencies?.map { it.fromState() },
      moduleDependencies = moduleDependencies.map { it.fromState() }
    )
}

public fun ModuleDetails.toState(): ModuleDetailsState =
  ModuleDetailsState(
    target = target.toState(),
    allTargetsIds = allTargetsIds.map { it.toState() },
    sources = sources.map { it.toState() },
    resources = resources.map { it.toState() },
    dependenciesSources = dependenciesSources.map { it.toState() },
    javacOptions = javacOptions?.toState(),
    outputPathUris = outputPathUris,
    libraryDependencies = libraryDependencies?.map { it.toState() },
    moduleDependencies = moduleDependencies.map { it.toState() }
  )


public data class DefaultMagicMetaModelState(
  public var projectDetailsState: ProjectDetailsState = ProjectDetailsState(),
  public var targetsDetailsForDocumentProviderState: TargetsDetailsForDocumentProviderState =
    TargetsDetailsForDocumentProviderState(),
  public var overlappingTargetsGraph: Map<BuildTargetIdentifierState, List<BuildTargetIdentifierState>> = emptyMap(),
  public var loadedTargetsStorageState: LoadedTargetsStorageState = LoadedTargetsStorageState(),
)
