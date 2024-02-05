package org.jetbrains.magicmetamodel.impl

import org.jetbrains.bsp.AndroidTargetType
import org.jetbrains.magicmetamodel.impl.workspacemodel.AndroidAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.IntermediateLibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.IntermediateModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.KotlinAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleCapabilities
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonLibrary
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonSdkInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ResourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.ScalaAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesPython
import java.net.URI
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
  var baseDirectory: String? = null,
) : ConvertableFromState<BuildTargetInfo> {
  override fun fromState(): BuildTargetInfo =
    BuildTargetInfo(
      id = id,
      displayName = displayName,
      dependencies = dependencies,
      capabilities = capabilities.fromState(),
      languageIds = languageIds,
      baseDirectory = baseDirectory,
    )
}

public fun BuildTargetInfo.toState(): BuildTargetInfoState = BuildTargetInfoState(
  id = id,
  displayName = displayName,
  dependencies = dependencies,
  capabilities = capabilities.toState(),
  languageIds = languageIds,
  baseDirectory = baseDirectory,
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

public data class ResourceRootState(
  var resourcePath: String = "",
  var rootType: String = "",
) {
  public fun toResourceRoot(): ResourceRoot = ResourceRoot(Path(resourcePath), rootType)
}

public fun ResourceRoot.toState(): ResourceRootState = ResourceRootState(
  resourcePath = resourcePath.toString(),
  rootType = rootType,
)

public fun GenericSourceRoot.toState(): SourceRootState = SourceRootState(
  sourcePath = sourcePath.toString(),
  rootType = rootType,
  excludedPaths = excludedPaths.map { it.toString() },
)

public data class LibraryState(
  var displayName: String = "",
  var sourceJars: List<String> = emptyList(),
  var classJars: List<String> = emptyList(),
) : ConvertableFromState<Library> {
  override fun fromState(): Library = Library(
    displayName = displayName,
    sourceJars = sourceJars,
    classJars = classJars,
  )

  internal fun toPythonLibrary(): PythonLibrary = PythonLibrary(sourceJars)
}

public fun Library.toState(): LibraryState = LibraryState(
  displayName = displayName,
  sourceJars = sourceJars,
  classJars = classJars,
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
    modulesDependencies = modulesDependencies.map { IntermediateModuleDependency(it) },
    librariesDependencies = librariesDependencies.map { IntermediateLibraryDependency(it) },
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
  var resourceRoots: List<ResourceRootState> = emptyList(),
  var libraries: List<LibraryState>? = null,
  var jvmJdkName: String? = null,
  val sdkInfo: PythonSdkInfoState? = null,
  var kotlinAddendum: KotlinAddendumState? = null,
  var scalaAddendum: ScalaAddendumState? = null,
  var javaAddendum: JavaAddendumState? = null,
  var androidAddendum: AndroidAddendumState? = null,
) : ConvertableFromState<Module> {
  public fun toJavaModule(): JavaModule = JavaModule(
    genericModuleInfo = module.fromState(),
    baseDirContentRoot = baseDirContentRoot?.fromState(),
    sourceRoots = sourceRoots.map { it.toJavaSourceRoot() },
    resourceRoots = resourceRoots.map { it.toResourceRoot() },
    moduleLevelLibraries = libraries?.map { it.fromState() },
    jvmJdkName = jvmJdkName,
    kotlinAddendum = kotlinAddendum?.fromState(),
    scalaAddendum = scalaAddendum?.fromState(),
    androidAddendum = androidAddendum?.fromState(),
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
  val kotlincOptions: List<String> = emptyList(),
) : ConvertableFromState<KotlinAddendum> {
  override fun fromState(): KotlinAddendum = KotlinAddendum(
    languageVersion = languageVersion,
    apiVersion = apiVersion,
    kotlincOptions = kotlincOptions,
  )
}

// TODO: What is it needed for? It's the same as ScalaAddendum
public data class ScalaAddendumState(
  var scalaSdkName: String = "",
) : ConvertableFromState<ScalaAddendum> {
  override fun fromState(): ScalaAddendum = ScalaAddendum(
    scalaSdkName = scalaSdkName,
  )
}

public data class JavaAddendumState(
  var languageVersion: String = "",
) : ConvertableFromState<JavaAddendum> {
  override fun fromState(): JavaAddendum = JavaAddendum(
    languageVersion = languageVersion
  )
}

public data class AndroidAddendumState(
  var androidSdkName: String = "",
  var androidTargetType: AndroidTargetType = AndroidTargetType.LIBRARY,
  val manifest: URI? = null,
  val resourceFolders: List<URI> = emptyList(),
) : ConvertableFromState<AndroidAddendum> {
  override fun fromState(): AndroidAddendum = AndroidAddendum(
    androidSdkName = androidSdkName,
    androidTargetType = androidTargetType,
    manifest = manifest,
    resourceFolders = resourceFolders,
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
  kotlincOptions = kotlincOptions,
)

public fun ScalaAddendum.toState(): ScalaAddendumState = ScalaAddendumState(
  scalaSdkName = scalaSdkName,
)

public fun AndroidAddendum.toState(): AndroidAddendumState = AndroidAddendumState(
  androidSdkName = androidSdkName,
  androidTargetType = androidTargetType,
  manifest = manifest,
  resourceFolders = resourceFolders,
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
  public var outputPathUris: List<String> = emptyList(),
)
