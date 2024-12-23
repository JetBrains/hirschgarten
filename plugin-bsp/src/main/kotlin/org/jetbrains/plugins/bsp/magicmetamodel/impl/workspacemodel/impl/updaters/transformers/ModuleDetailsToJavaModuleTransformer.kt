package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bsp.protocol.utils.extractAndroidBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractKotlinBuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget
import org.jetbrains.plugins.bsp.config.bspProjectName
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.plugins.bsp.utils.StringUtils
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import org.jetbrains.plugins.bsp.workspacemodel.entities.AndroidAddendum
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.IntermediateModuleDependency
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaAddendum
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaModule
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.plugins.bsp.workspacemodel.entities.KotlinAddendum
import org.jetbrains.plugins.bsp.workspacemodel.entities.ResourceRoot
import org.jetbrains.plugins.bsp.workspacemodel.entities.ScalaAddendum
import java.nio.file.Path
import kotlin.io.path.toPath

internal class ModuleDetailsToJavaModuleTransformer(
  targetsMap: Map<BuildTargetIdentifier, BuildTargetInfo>,
  nameProvider: TargetNameReformatProvider,
  private val projectBasePath: Path,
  private val project: Project,
  private val isAndroidSupportEnabled: Boolean = false,
) : ModuleDetailsToModuleTransformer<JavaModule>(targetsMap, nameProvider) {
  override val type = ModuleTypeId("JAVA_MODULE")

  private val resourcesItemToJavaResourceRootTransformer = ResourcesItemToJavaResourceRootTransformer()

  override fun transform(inputEntity: ModuleDetails): JavaModule =
    JavaModule(
      genericModuleInfo = toGenericModuleInfo(inputEntity),
      baseDirContentRoot = toBaseDirContentRoot(inputEntity),
      sourceRoots = toJavaSourceRoots(inputEntity),
      resourceRoots = toResourceRoots(inputEntity),
      moduleLevelLibraries =
        if (inputEntity.libraryDependencies == null) {
          DependencySourcesItemToLibraryTransformer
            .transform(
              inputEntity.dependenciesSources.map {
                DependencySourcesAndJvmClassPaths(it, inputEntity.toJvmClassPaths())
              },
            )
        } else {
          null
        },
      // Any java module must be assigned a jdk if there is any available.
      jvmJdkName = inputEntity.toJdkNameOrDefault(),
      jvmBinaryJars = inputEntity.jvmBinaryJars.flatMap { it.jars }.map { it.safeCastToURI().toPath() },
      kotlinAddendum = toKotlinAddendum(inputEntity),
      scalaAddendum = toScalaAddendum(inputEntity),
      javaAddendum = toJavaAddendum(inputEntity),
      androidAddendum = if (isAndroidSupportEnabled) toAndroidAddendum(inputEntity) else null,
      workspaceModelEntitiesFolderMarker = inputEntity.workspaceModelEntitiesFolderMarker,
    )

  private fun toJavaSourceRoots(inputEntity: ModuleDetails): List<JavaSourceRoot> =
    SourcesItemToJavaSourceRootTransformer(inputEntity.workspaceModelEntitiesFolderMarker).transform(
      inputEntity.sources.map {
        BuildTargetAndSourceItem(
          buildTarget = inputEntity.target,
          sourcesItem = it,
        )
      },
    )

  private fun toResourceRoots(inputEntity: ModuleDetails): List<ResourceRoot> =
    resourcesItemToJavaResourceRootTransformer.transform(
      inputEntity.resources.map {
        BuildTargetAndResourcesItem(
          buildTarget = inputEntity.target,
          resourcesItem = it,
        )
      },
    )

  private fun ModuleDetails.toJvmClassPaths() =
    (this.javacOptions?.classpath.orEmpty() + this.scalacOptions?.classpath.orEmpty()).distinct()

  override fun toGenericModuleInfo(inputEntity: ModuleDetails): GenericModuleInfo {
    val bspModuleDetails =
      BspModuleDetails(
        target = inputEntity.target,
        dependencySources = inputEntity.dependenciesSources,
        type = type,
        javacOptions = inputEntity.javacOptions,
        associates = toAssociates(inputEntity),
        libraryDependencies = inputEntity.libraryDependencies,
        moduleDependencies = inputEntity.moduleDependencies,
        scalacOptions = inputEntity.scalacOptions,
      )

    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails).applyHACK(inputEntity, projectBasePath)
  }

  private fun GenericModuleInfo.applyHACK(inputEntity: ModuleDetails, projectBasePath: Path): GenericModuleInfo {
    val dummyJavaModuleDependencies =
      calculateDummyJavaModuleNames(calculateDummyJavaSourceRoots(toJavaSourceRoots(inputEntity)), projectBasePath)
        .filter { it.isNotEmpty() }
        .map { IntermediateModuleDependency(it) }
    return this.copy(modulesDependencies = modulesDependencies + dummyJavaModuleDependencies)
  }

  private fun ModuleDetails.toJdkNameOrDefault(): String? = toJdkName() ?: defaultJdkName

  private fun ModuleDetails.toJdkName(): String? = extractJvmBuildTarget(this.target).toJdkName()

  private fun JvmBuildTarget?.toJdkName(): String? = this?.javaHome?.let { project.bspProjectName.projectNameToJdkName(it) }

  private fun toKotlinAddendum(inputEntity: ModuleDetails): KotlinAddendum? {
    val kotlinBuildTarget = extractKotlinBuildTarget(inputEntity.target)
    return if (kotlinBuildTarget != null) {
      with(kotlinBuildTarget) {
        KotlinAddendum(
          languageVersion = languageVersion,
          apiVersion = apiVersion,
          kotlincOptions = kotlincOptions,
        )
      }
    } else {
      null
    }
  }

  private fun toScalaAddendum(inputEntity: ModuleDetails): ScalaAddendum? {
    val scalaBuildTarget = extractScalaBuildTarget(inputEntity.target)
    val version = scalaBuildTarget?.scalaVersion?.scalaVersionToScalaSdkName() ?: return null
    return ScalaAddendum(
      scalaSdkName = version,
    )
  }

  private fun toJavaAddendum(inputEntity: ModuleDetails): JavaAddendum? =
    extractJvmBuildTarget(inputEntity.target)?.javaVersion?.let {
      JavaAddendum(
        languageVersion = it,
        javacOptions = inputEntity.javacOptions?.options.orEmpty(),
      )
    }

  private fun toAndroidAddendum(inputEntity: ModuleDetails): AndroidAddendum? {
    val androidBuildTarget = extractAndroidBuildTarget(inputEntity.target) ?: return null
    return with(androidBuildTarget) {
      AndroidAddendum(
        androidSdkName = androidJar.androidJarToAndroidSdkName(),
        androidTargetType = androidTargetType,
        manifest = manifest?.safeCastToURI()?.toPath(),
        manifestOverrides = manifestOverrides,
        resourceDirectories = resourceDirectories.map { it.safeCastToURI().toPath() },
        resourceJavaPackage = resourceJavaPackage,
        assetsDirectories = assetsDirectories.map { it.safeCastToURI().toPath() },
        apk = apk?.safeCastToURI()?.toPath(),
      )
    }
  }

  private fun toAssociates(inputEntity: ModuleDetails): List<BuildTargetIdentifier> {
    val kotlinBuildTarget = extractKotlinBuildTarget(inputEntity.target)
    return kotlinBuildTarget
      ?.associates
      ?.distinct()
      ?: emptyList()
  }
}

fun String.scalaVersionToScalaSdkName(): String = "scala-sdk-$this"

fun String.projectNameToBaseJdkName(): String = "$this-jdk"

fun String.projectNameToJdkName(javaHomeUri: String): String = projectNameToBaseJdkName() + "-" + StringUtils.md5Hash(javaHomeUri, 5)

fun String.androidJarToAndroidSdkName(): String = "android-sdk-" + StringUtils.md5Hash(this, 5)
