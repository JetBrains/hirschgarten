package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import org.jetbrains.bsp.utils.extractJvmBuildTarget
import org.jetbrains.bsp.utils.extractKotlinBuildTarget
import org.jetbrains.bsp.utils.extractScalaBuildTarget
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.KotlinAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ScalaAddendum
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.toPath

public data class KotlinBuildTarget(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
  val associates: List<BuildTargetIdentifier>,
  var jvmBuildTarget: JvmBuildTarget? = null,
)

internal class ModuleDetailsToJavaModuleTransformer(
  moduleNameProvider: ModuleNameProvider,
  private val projectBasePath: Path,
) : ModuleDetailsToModuleTransformer<JavaModule>(moduleNameProvider) {
  override val type = "JAVA_MODULE"

  private val sourcesItemToJavaSourceRootTransformer = SourcesItemToJavaSourceRootTransformer(projectBasePath)
  private val resourcesItemToJavaResourceRootTransformer = ResourcesItemToJavaResourceRootTransformer(projectBasePath)

  override fun transform(inputEntity: ModuleDetails): JavaModule =
    JavaModule(
      genericModuleInfo = toGenericModuleInfo(inputEntity),
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
              inputEntity.javacOptions,
            )
          }) else null,
      compilerOutput = toCompilerOutput(inputEntity),
      jvmJdkName = toJdkName(inputEntity),
      kotlinAddendum = toKotlinAddendum(inputEntity),
      scalaAddendum = toScalaAddendum(inputEntity),
    )

  override fun toGenericModuleInfo(inputEntity: ModuleDetails): GenericModuleInfo {
    val bspModuleDetails = BspModuleDetails(
      target = inputEntity.target,
      dependencySources = inputEntity.dependenciesSources,
      type = type,
      javacOptions = inputEntity.javacOptions,
      pythonOptions = null,
      associates = toAssociates(inputEntity),
      libraryDependencies = inputEntity.libraryDependencies,
      moduleDependencies = inputEntity.moduleDependencies,
    )

    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails).applyHACK(inputEntity, projectBasePath)
  }

  private fun GenericModuleInfo.applyHACK(inputEntity: ModuleDetails, projectBasePath: Path): GenericModuleInfo {
    val dummyJavaModuleDependencies =
      calculateDummyJavaModuleNames(inputEntity.calculateDummyJavaSourceRoots(), projectBasePath)
        .filter { it.isNotEmpty() }
        .map { ModuleDependency(it) }
    return this.copy(modulesDependencies = modulesDependencies + dummyJavaModuleDependencies)
  }

  private fun ModuleDetails.calculateDummyJavaSourceRoots(): List<Path> =
    sources.mapNotNull { it.roots }.flatten().map { URI.create(it) }.map { it.toPath() }

  private fun toCompilerOutput(inputEntity: ModuleDetails): Path? =
    inputEntity.javacOptions?.classDirectory?.let { URI(it).toPath() }

  private fun toJdkName(inputEntity: ModuleDetails): String? {
    val jvmBuildTarget = extractJvmBuildTarget(inputEntity.target)
    return jvmBuildTarget?.javaVersion?.javaVersionToJdkName(projectBasePath.name)
  }

  private fun toKotlinAddendum(inputEntity: ModuleDetails): KotlinAddendum? {
    val kotlinBuildTarget = extractKotlinBuildTarget(inputEntity.target)
    return if (kotlinBuildTarget != null)
      with(kotlinBuildTarget) {
        KotlinAddendum(
          languageVersion = languageVersion,
          apiVersion = apiVersion,
          kotlincOptions = kotlincOptions,
        )
      } else null
  }

  private fun toScalaAddendum(inputEntity: ModuleDetails): ScalaAddendum? {
    val scalaBuildTarget = extractScalaBuildTarget(inputEntity.target)
    val version = ScalaUtil.scalaVersionToScalaSdkName(scalaBuildTarget?.scalaVersion) ?: return null
    val jars = scalaBuildTarget?.jars ?: return null
    return ScalaAddendum(
      scalaSdkName = version,
      scalaSdkLibraries = jars.map { Paths.get(URI(it)).toFile().name },
    )
  }

  private fun toAssociates(inputEntity: ModuleDetails): List<BuildTargetId> {
    val kotlinBuildTarget = extractKotlinBuildTarget(inputEntity.target)
    return kotlinBuildTarget?.associates?.map { it.uri } ?: emptyList()
  }
}

public fun String.javaVersionToJdkName(projectName: String): String = "$projectName-$this"

public object ScalaUtil {
  public fun scalaVersionToScalaSdkName(version: String?): String? = version?.let { "scala-sdk-$version" }
}
