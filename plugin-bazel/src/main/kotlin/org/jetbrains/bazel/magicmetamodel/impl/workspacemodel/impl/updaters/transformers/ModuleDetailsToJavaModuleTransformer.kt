package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bazel.config.bazelProjectName
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.utils.StringUtils
import org.jetbrains.bazel.utils.safeCastToURI
import org.jetbrains.bazel.workspacemodel.entities.AndroidAddendum
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.IntermediateModuleDependency
import org.jetbrains.bazel.workspacemodel.entities.JavaAddendum
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.workspacemodel.entities.KotlinAddendum
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bazel.workspacemodel.entities.ScalaAddendum
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractAndroidBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractKotlinBuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal class ModuleDetailsToJavaModuleTransformer(
  targetsMap: Map<Label, BuildTargetInfo>,
  fileToTarget: Map<URI, List<Label>>,
  nameProvider: TargetNameReformatProvider,
  projectBasePath: Path,
  private val project: Project,
  private val isAndroidSupportEnabled: Boolean = false,
) {
  private val bspModuleDetailsToModuleTransformer = BspModuleDetailsToModuleTransformer(targetsMap, nameProvider)
  private val type = ModuleTypeId("JAVA_MODULE")
  private val resourcesItemToJavaResourceRootTransformer = ResourcesItemToJavaResourceRootTransformer()
  private val javaModuleToDummyJavaModulesTransformerHACK =
    JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, fileToTarget, project)

  fun transform(inputEntity: ModuleDetails): List<JavaModule> {
    val javaModule =
      JavaModule(
        genericModuleInfo = toGenericModuleInfo(inputEntity),
        baseDirContentRoot = toBaseDirContentRoot(inputEntity),
        sourceRoots = toJavaSourceRoots(inputEntity),
        resourceRoots = toResourceRoots(inputEntity),
        // Any java module must be assigned a jdk if there is any available.
        jvmJdkName = inputEntity.toJdkNameOrDefault(),
        jvmBinaryJars = inputEntity.jvmBinaryJars.flatMap { it.jars }.map { it.safeCastToURI().toPath() },
        kotlinAddendum = toKotlinAddendum(inputEntity),
        scalaAddendum = toScalaAddendum(inputEntity),
        javaAddendum = toJavaAddendum(inputEntity),
        androidAddendum = if (isAndroidSupportEnabled) toAndroidAddendum(inputEntity) else null,
      )

    val dummyModulesResult = javaModuleToDummyJavaModulesTransformerHACK.transform(javaModule)
    return when (dummyModulesResult) {
      is JavaModuleToDummyJavaModulesTransformerHACK.DummyModulesToAdd -> {
        val dummyModules = dummyModulesResult.dummyModules
        val dummyModuleDependencies = dummyModules.map { IntermediateModuleDependency(it.genericModuleInfo.name) }
        val javaModuleWithDummyDependencies =
          javaModule.copy(
            genericModuleInfo =
              javaModule.genericModuleInfo.copy(
                modulesDependencies =
                  javaModule.genericModuleInfo.modulesDependencies + dummyModuleDependencies,
              ),
          )
        listOf(javaModuleWithDummyDependencies) + dummyModules
      }
      is JavaModuleToDummyJavaModulesTransformerHACK.MergedSourceRoots -> {
        val javaModuleWithMergedSourceRoots =
          javaModule.copy(
            sourceRoots = dummyModulesResult.mergedSourceRoots,
          )
        listOf(javaModuleWithMergedSourceRoots)
      }
    }
  }

  private fun toJavaSourceRoots(inputEntity: ModuleDetails): List<JavaSourceRoot> =
    SourcesItemToJavaSourceRootTransformer().transform(inputEntity.target)

  private fun toResourceRoots(inputEntity: ModuleDetails): List<ResourceRoot> =
    resourcesItemToJavaResourceRootTransformer.transform(inputEntity.target)

  private fun toGenericModuleInfo(inputEntity: ModuleDetails): GenericModuleInfo {
    val bspModuleDetails =
      BspModuleDetails(
        target = inputEntity.target,
        type = type,
        javacOptions = inputEntity.javacOptions,
        associates = toAssociates(inputEntity),
        libraryDependencies = inputEntity.libraryDependencies,
        moduleDependencies = inputEntity.moduleDependencies,
        scalacOptions = inputEntity.scalacOptions,
      )

    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails)
  }

  private fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      // TODO https://youtrack.jetbrains.com/issue/BAZEL-635
      path = (inputEntity.target.baseDirectory ?: "file:///todo").safeCastToURI().toPath(),
    )

  private fun ModuleDetails.toJdkNameOrDefault(): String? = toJdkName() ?: defaultJdkName

  private fun ModuleDetails.toJdkName(): String? = extractJvmBuildTarget(this.target).toJdkName()

  private fun JvmBuildTarget?.toJdkName(): String? = this?.javaHome?.let { project.bazelProjectName.projectNameToJdkName(it) }

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

  private fun toAssociates(inputEntity: ModuleDetails): List<Label> {
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
