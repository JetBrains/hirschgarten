package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import org.jetbrains.bsp.utils.extractAndroidBuildTarget
import org.jetbrains.bsp.utils.extractJvmBuildTarget
import org.jetbrains.bsp.utils.extractKotlinBuildTarget
import org.jetbrains.bsp.utils.extractScalaBuildTarget
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.AndroidAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.KotlinAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ScalaAddendum
import java.net.URI
import java.nio.file.Path
import java.security.MessageDigest
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
  private val isAndroidSupportEnabled: Boolean = false,
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
      resourceRoots = resourcesItemToJavaResourceRootTransformer.transform(inputEntity.resources.map {
        BuildTargetAndResourcesItem(
          inputEntity.target,
          it,
        )
      }),
      moduleLevelLibraries = if (inputEntity.libraryDependencies == null)
        DependencySourcesItemToLibraryTransformer
          .transform(inputEntity.dependenciesSources.map {
            DependencySourcesAndJvmClassPaths(it, inputEntity.toJvmClassPaths())
          }) else null,
      // Any java module must be assigned a jdk if there is any available.
      jvmJdkName = inputEntity.toJdkNameOrDefault(),
      kotlinAddendum = toKotlinAddendum(inputEntity),
      scalaAddendum = toScalaAddendum(inputEntity),
      javaAddendum = toJavaAddendum(inputEntity),
      androidAddendum = if (isAndroidSupportEnabled) toAndroidAddendum(inputEntity) else null,
    )

  private fun ModuleDetails.toJvmClassPaths() =
    (this.javacOptions?.classpath.orEmpty() + this.scalacOptions?.classpath.orEmpty()).distinct()

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
      scalacOptions = inputEntity.scalacOptions,
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

  private fun ModuleDetails.toJdkNameOrDefault(): String? =
    toJdkName() ?: defaultJdkName

  private fun ModuleDetails.toJdkName(): String? =
    extractJvmBuildTarget(this.target).toJdkName()

  private fun JvmBuildTarget?.toJdkName(): String? =
    this?.javaHome?.let { projectBasePath.name.projectNameToJdkName(it) }

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
    val version = scalaBuildTarget?.scalaVersion?.scalaVersionToScalaSdkName() ?: return null
    return ScalaAddendum(
      scalaSdkName = version
    )
  }

  private fun toJavaAddendum(inputEntity: ModuleDetails): JavaAddendum? =
    extractJvmBuildTarget(inputEntity.target)?.javaVersion?.let { JavaAddendum(languageVersion = it) }

  private fun toAndroidAddendum(inputEntity: ModuleDetails): AndroidAddendum? {
    val androidBuildTarget = extractAndroidBuildTarget(inputEntity.target) ?: return null
    return with(androidBuildTarget) {
      AndroidAddendum(
        androidSdkName = androidJar.androidJarToAndroidSdkName(),
        androidTargetType = androidTargetType,
      )
    }
  }

  private fun toAssociates(inputEntity: ModuleDetails): List<BuildTargetId> {
    val kotlinBuildTarget = extractKotlinBuildTarget(inputEntity.target)
    return kotlinBuildTarget?.associates?.map { it.uri } ?: emptyList()
  }
}

public fun String.javaVersionToJdkName(projectName: String): String = "$projectName-$this"

public fun String.scalaVersionToScalaSdkName(): String = "scala-sdk-$this"

public fun String.projectNameToBaseJdkName(): String = "$this-jdk"

public fun String.projectNameToJdkName(javaHomeUri: String): String =
  projectNameToBaseJdkName() + "-" + javaHomeUri.md5Hash()

public fun URI.androidJarToAndroidSdkName(): String = "android-sdk-" + this.toString().md5Hash()

private fun String.md5Hash(): String {
  val md = MessageDigest.getInstance("MD5")
  val hash = md.digest(this.toByteArray())
  return hash.toHexString().substring(0, 5)
}
