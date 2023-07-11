package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JvmJdkInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.KotlinAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleDependency
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.toPath

public data class KotlincOpts(
  val includeStdlibs: String,
  val javaParameters: Boolean,
  val jvmTarget: String,
  val warn : String,
  val xAllowResultReturnType : Boolean,
  val xBackendThreads: Int,
  val xEmitJvmTypeAnnotations: Boolean,
  val xEnableIncrementalCompilation: Boolean,
  val xExplicitApiMode: String,
  val xInlineClasses: Boolean,
  val xJvmDefault: String,
  val xLambdas: String,
  val xMultiPlatform: Boolean,
  val xNoCallAssertions: Boolean,
  val xNoOptimize: Boolean,
  val xNoOptimizedCallableReferences: Boolean,
  val xNoParamAssertions: Boolean,
  val xNoReceiverAssertions: Boolean,
  val xOptinList: List<String>,
  val xReportPerf: Boolean,
  val xSamConversions: String,
  val xSkipPrereleaseCheck: Boolean,
  val xUseFirLt: Boolean,
  val xUseK2: Boolean,
)

public data class KotlinBuildTarget(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: KotlincOpts?,
  val associates: List<BuildTargetIdentifier>,
  var jvmBuildTarget: JvmBuildTarget? = null
)

internal class ModuleDetailsToJavaModuleTransformer(
  moduleNameProvider: ModuleNameProvider,
  private val projectBasePath: Path,
) : WorkspaceModelEntityTransformer<ModuleDetails, JavaModule> {

  private val bspModuleDetailsToModuleTransformer = BspModuleDetailsToModuleTransformer(moduleNameProvider)
  private val sourcesItemToJavaSourceRootTransformer = SourcesItemToJavaSourceRootTransformer(projectBasePath)
  private val resourcesItemToJavaResourceRootTransformer = ResourcesItemToJavaResourceRootTransformer(projectBasePath)

  override fun transform(inputEntity: ModuleDetails): JavaModule =
    JavaModule(
      module = toModule(inputEntity),
      baseDirContentRoot = toBaseDirContentRoot(inputEntity),
      sourceRoots = sourcesItemToJavaSourceRootTransformer.transform(inputEntity.sources.map {
        BuildTargetAndSourceItem(
          inputEntity.target,
          it,
        )
      }),
      resourceRoots = resourcesItemToJavaResourceRootTransformer.transform(inputEntity.resources),
      moduleLevelLibraries = if (inputEntity.libraryDependencies == null)
          DependencySourcesItemToLibraryTransformer
             .transform(inputEntity.dependenciesSources.map {
                 DependencySourcesAndJavacOptions(
                   it,
                   inputEntity.javacOptions
                 )
             }) else null,
      compilerOutput = toCompilerOutput(inputEntity),
      jvmJdkInfo = toJdkInfo(inputEntity),
      kotlinAddendum = toKotlinAddendum(inputEntity)
    )

  private fun toModule(inputEntity: ModuleDetails): Module {
    val bspModuleDetails = BspModuleDetails(
      target = inputEntity.target,
      allTargetsIds = inputEntity.allTargetsIds,
      dependencySources = inputEntity.dependenciesSources,
      type = type,
      javacOptions = inputEntity.javacOptions,
      associates = toAssociates(inputEntity),
      libraryDependencies = inputEntity.libraryDependencies,
      moduleDependencies = inputEntity.moduleDependencies
    )

    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails).applyHACK(inputEntity, projectBasePath)
  }

  private fun Module.applyHACK(inputEntity: ModuleDetails, projectBasePath: Path): Module {
    val dummyJavaModuleDependencies = calculateDummyJavaModuleNames(inputEntity, projectBasePath)
      .map { ModuleDependency(it) }
    return this.copy(modulesDependencies = modulesDependencies + dummyJavaModuleDependencies)
  }

  private fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      // TODO what if null?
      url = URI.create(inputEntity.target.baseDirectory ?: "file:///todo").toPath(),
      excludedPaths = inputEntity.outputPathUris.map { URI.create(it).toPath() },
    )

  private fun toCompilerOutput(inputEntity: ModuleDetails): Path? =
    inputEntity.javacOptions?.classDirectory?.let { URI(it).toPath() }

  private fun toJdkInfo(inputEntity: ModuleDetails): JvmJdkInfo? {
    val jvmBuildTarget = extractJvmBuildTarget(inputEntity.target)
    return if (jvmBuildTarget != null)
      JvmJdkInfo(
        name = jvmBuildTarget.javaVersion.javaVersionToJdkName(projectBasePath.name),
        javaHome = jvmBuildTarget.javaHome
      )
    else null
  }

  private fun toKotlinAddendum(inputEntity: ModuleDetails): KotlinAddendum? {
    val kotlinBuildTarget = extractKotlinBuildTarget(inputEntity.target)
    return if (kotlinBuildTarget != null)
      with(kotlinBuildTarget) {
        KotlinAddendum(
          languageVersion = languageVersion,
          apiVersion = apiVersion,
          kotlincOptions = kotlincOptions
        )
      } else null
  }

  private fun toAssociates(inputEntity: ModuleDetails): List<BuildTargetIdentifier> {
    val kotlinBuildTarget = extractKotlinBuildTarget(inputEntity.target)
    return kotlinBuildTarget?.associates ?: emptyList()
  }

  companion object {
    private const val type = "JAVA_MODULE"
  }

}

// TODO ugly, but we need to change it anyway
public fun extractJvmBuildTarget(target: BuildTarget): JvmBuildTarget? =
  when (target.dataKind) {
    BuildTargetDataKind.JVM ->
      when (target.data) {
        is JvmBuildTarget -> target.data as JvmBuildTarget
        else -> Gson().fromJson(target.data as JsonObject, JvmBuildTarget::class.java)
      }

    "kotlin" -> extractKotlinBuildTargetIfIsKotlinDataKind(target.data)?.jvmBuildTarget
    else -> null
  }

public fun extractKotlinBuildTarget(target: BuildTarget): KotlinBuildTarget? =
  when (target.dataKind) {
    "kotlin" -> extractKotlinBuildTargetIfIsKotlinDataKind(target.data)
    else -> null
  }

public fun extractKotlinBuildTargetIfIsKotlinDataKind(data: Any): KotlinBuildTarget? =
  when (data) {
    is KotlinBuildTarget -> data
    else -> Gson().fromJson(data as JsonObject, KotlinBuildTarget::class.java)
  }

public fun String.javaVersionToJdkName(projectName: String): String = "$projectName-$this"
