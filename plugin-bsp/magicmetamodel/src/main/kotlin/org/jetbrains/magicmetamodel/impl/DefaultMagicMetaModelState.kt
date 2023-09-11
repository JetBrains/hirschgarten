package org.jetbrains.magicmetamodel.impl

import com.google.gson.Gson
import org.jetbrains.magicmetamodel.impl.workspacemodel.*
import kotlin.io.path.Path

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

public data class BuildTargetInfoState(
  var id: BuildTargetId = "",
  var displayName: String? = null,
  var dependencies: List<BuildTargetId> = emptyList(),
  var capabilities: ModuleCapabilitiesState = ModuleCapabilitiesState(),
  var languageIds: List<String> = emptyList(),
) : ConvertableFromState<BuildTargetInfo> {
  override fun fromState(): BuildTargetInfo =
    BuildTargetInfo(
      id = id,
      displayName = displayName,
      dependencies = dependencies,
      capabilities = capabilities.fromState(),
      languageIds = languageIds,
    )
}

public fun BuildTargetInfo.toState(): BuildTargetInfoState = BuildTargetInfoState(
  id = id,
  displayName = displayName,
  dependencies = dependencies,
  capabilities = capabilities.toState(),
  languageIds = languageIds,
)

public data class ContentRootState(
  var path: String = "",
  var excludedPaths: List<String> = emptyList(),
) : ConvertableFromState<ContentRoot> {
  override fun fromState(): ContentRoot =
    ContentRoot(
      path = Path(path),
      excludedPaths = excludedPaths.map { Path(it) },
    )
}

public fun ContentRoot.toState(): ContentRootState =
  ContentRootState(
    path = path.toString(),
    excludedPaths = excludedPaths.map { it.toString() },
  )

public data class SourceRootState(
  var sourcePath: String = "",
  var generated: Boolean = false,
  var packagePrefix: String = "",
  var rootType: String = "",
  var excludedPaths: List<String> = emptyList(),
) {
  public fun toJavaSourceRoot(): JavaSourceRoot = JavaSourceRoot(
    sourcePath = Path(sourcePath),
    generated = generated,
    packagePrefix = packagePrefix,
    rootType = rootType,
    excludedPaths = excludedPaths.map { Path(it) },
  )

  public fun toGenericSourceRoot(): GenericSourceRoot = GenericSourceRoot(
    sourcePath = Path(sourcePath),
    rootType = rootType,
    excludedPaths = excludedPaths.map { Path(it) },
  )
}

public fun JavaSourceRoot.toState(): SourceRootState = SourceRootState(
  sourcePath = sourcePath.toString(),
  generated = generated,
  packagePrefix = packagePrefix,
  rootType = rootType,
  excludedPaths = excludedPaths.map { it.toString() },
)

internal fun String.toResourceRoot() = ResourceRoot(Path(this))

public fun GenericSourceRoot.toState(): SourceRootState = SourceRootState(
  sourcePath = sourcePath.toString(),
  rootType = rootType,
  excludedPaths = excludedPaths.map { it.toString() },
)

public data class LibraryState(
  var displayName: String = "",
  var sourceJars: List<String> = listOf(),
  var classeJars: List<String> = listOf(),
) : ConvertableFromState<Library> {
  override fun fromState(): Library = Library(
    displayName = displayName,
    sourceJars = sourceJars,
    classJars = classeJars,
  )

  internal fun toPythonLibrary(): PythonLibrary = PythonLibrary(sourceJars)
}

public fun Library.toState(): LibraryState = LibraryState(
  displayName = displayName,
  sourceJars = sourceJars,
  classeJars = classJars,
)

public fun PythonLibrary.toState(): LibraryState = LibraryState(
  sourceJars = sources,
)

public data class GenericModuleInfoState(
  var name: String = "",
  var type: String = "",
  var modulesDependencies: List<String> = emptyList(),
  var librariesDependencies: List<String> = emptyList(),
  var capabilities: ModuleCapabilitiesState = ModuleCapabilitiesState(),
  var languageIds: List<String> = emptyList(),
) : ConvertableFromState<GenericModuleInfo> {
  override fun fromState(): GenericModuleInfo = GenericModuleInfo(
    name = name,
    type = type,
    modulesDependencies = modulesDependencies.map { ModuleDependency(it) },
    librariesDependencies = librariesDependencies.map { LibraryDependency(it) },
    capabilities = capabilities.fromState(),
    languageIds = languageIds,
  )
}

public fun GenericModuleInfo.toState(): GenericModuleInfoState = GenericModuleInfoState(
  name = name,
  type = type,
  modulesDependencies = modulesDependencies.map { it.moduleName },
  librariesDependencies = librariesDependencies.map { it.libraryName },
  capabilities = capabilities.toState(),
  languageIds = languageIds,
)

// TODO: Provide more generic structure for module with support for other languages
public data class ModuleState(
  var module: GenericModuleInfoState = GenericModuleInfoState(),
  var baseDirContentRoot: ContentRootState? = null,
  var sourceRoots: List<SourceRootState> = emptyList(),
  var resourceRoots: List<String> = emptyList(),
  var libraries: List<LibraryState>? = null,
  var compilerOutput: String? = null,
  var jvmJdkName: String? = null,
  val sdkInfo: PythonSdkInfoState? = null,
  var kotlinAddendum: KotlinAddendumState? = null,
) : ConvertableFromState<Module> {
  public fun toJavaModule(): JavaModule = JavaModule(
    genericModuleInfo = module.fromState(),
    baseDirContentRoot = baseDirContentRoot?.fromState(),
    sourceRoots = sourceRoots.map { it.toJavaSourceRoot() },
    resourceRoots = resourceRoots.map { it.toResourceRoot() },
    moduleLevelLibraries = libraries?.map { it.fromState() },
    compilerOutput = compilerOutput?.let { Path(it) },
    jvmJdkName = jvmJdkName,
    kotlinAddendum = kotlinAddendum?.fromState(),
  )

  public fun toPythonModule(): PythonModule = PythonModule(
    module = module.fromState(),
    sourceRoots = sourceRoots.map { it.toGenericSourceRoot() },
    libraries = libraries?.map { it.toPythonLibrary() }.orEmpty(),
    resourceRoots = resourceRoots.map { it.toResourceRoot() },
    sdkInfo = sdkInfo?.fromState(),
  )

  override fun fromState(): Module =
    if (module.languageIds.includesPython())
      toPythonModule()
    else
      toJavaModule()
}

public data class PythonSdkInfoState(
  var version: String = "",
  var originalName: String = "",
) : ConvertableFromState<PythonSdkInfo> {
  override fun fromState(): PythonSdkInfo = PythonSdkInfo(
    version = version,
    originalName = originalName,
  )
}

public fun PythonSdkInfo.toState(): PythonSdkInfoState = PythonSdkInfoState(
  version = version,
  originalName = originalName,
)

public data class KotlinAddendumState(
  var languageVersion: String = "",
  val apiVersion: String = "",
  val kotlincOptions: String = "",
) : ConvertableFromState<KotlinAddendum> {
  override fun fromState(): KotlinAddendum = KotlinAddendum(
    languageVersion = languageVersion,
    apiVersion = apiVersion,
    kotlincOptions = Gson().fromJson(kotlincOptions, KotlincOpts::class.java),
  )
}

public data class ModuleCapabilitiesState(
  var canRun: Boolean = false,
  var canTest: Boolean = false,
  var canCompile: Boolean = false,
  var canDebug: Boolean = false,
) : ConvertableFromState<ModuleCapabilities> {
  override fun fromState(): ModuleCapabilities = ModuleCapabilities(
    canRun = canRun,
    canTest = canTest,
    canCompile = canCompile,
    canDebug = canDebug,
  )
}

public fun KotlinAddendum.toState(): KotlinAddendumState = KotlinAddendumState(
  languageVersion = languageVersion,
  apiVersion = apiVersion,
  kotlincOptions = Gson().toJson(kotlincOptions),
)

public fun ModuleCapabilities.toState(): ModuleCapabilitiesState = ModuleCapabilitiesState(
  canRun = canRun,
  canTest = canTest,
  canCompile = canCompile,
  canDebug = canDebug,
)




public data class DefaultMagicMetaModelState(
  public var targets: List<BuildTargetInfoState> = emptyList(),
  public var libraries: List<LibraryState>? = null,
  public var unloadedTargets: Map<BuildTargetId, ModuleState> = emptyMap(),
  public var targetsDetailsForDocumentProviderState: TargetsDetailsForDocumentProviderState =
    TargetsDetailsForDocumentProviderState(),
  public var overlappingTargetsGraph: Map<BuildTargetId, Set<BuildTargetId>> = emptyMap(),
  public var loadedTargetsStorageState: LoadedTargetsStorageState = LoadedTargetsStorageState(),
  public var excludedPaths: List<String> = emptyList(),
)
