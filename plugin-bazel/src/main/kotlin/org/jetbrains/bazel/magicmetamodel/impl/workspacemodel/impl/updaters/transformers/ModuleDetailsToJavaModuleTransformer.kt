package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.bazelProjectName
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.AndroidAddendum
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.IntermediateModuleDependency
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaAddendum
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.KotlinAddendum
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ResourceRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ScalaAddendum
import org.jetbrains.bazel.utils.StringUtils
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.utils.extractAndroidBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractKotlinBuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget
import java.nio.file.Path

internal class ModuleDetailsToJavaModuleTransformer(
  targetsMap: Map<Label, BuildTarget>,
  fileToTargetWithoutLowPrioritySharedSources: Map<Path, List<Label>>,
  projectBasePath: Path,
  private val project: Project,
  private val isAndroidSupportEnabled: Boolean = false,
) {
  private val bspModuleDetailsToModuleTransformer = BspModuleDetailsToModuleTransformer(targetsMap, project)
  private val type = ModuleTypeId("JAVA_MODULE")
  private val resourcesItemToJavaResourceRootTransformer = ResourcesItemToJavaResourceRootTransformer()
  private val javaModuleToDummyJavaModulesTransformerHACK =
    JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, fileToTargetWithoutLowPrioritySharedSources, project)

  fun transform(inputEntity: ModuleDetails): List<JavaModule> {
    val javaModule =
      JavaModule(
        genericModuleInfo = toGenericModuleInfo(inputEntity),
        baseDirContentRoot = toBaseDirContentRoot(inputEntity),
        sourceRoots = toJavaSourceRoots(inputEntity),
        resourceRoots = toResourceRoots(inputEntity),
        // Any java module must be assigned a jdk if there is any available.
        jvmJdkName = inputEntity.toJdkNameOrDefault(),
        jvmBinaryJars = inputEntity.jvmBinaryJars.flatMap { it.jars },
        kotlinAddendum = toKotlinAddendum(inputEntity),
        scalaAddendum = toScalaAddendum(inputEntity),
        javaAddendum = toJavaAddendum(inputEntity),
        androidAddendum = if (isAndroidSupportEnabled) toAndroidAddendum(inputEntity) else null,
        compiledClassesPath = resolveCompiledClassesPathForJVMLanguage(inputEntity),
      )

    val dummyModulesResult = javaModuleToDummyJavaModulesTransformerHACK.transform(javaModule)
    return when (dummyModulesResult) {
      is JavaModuleToDummyJavaModulesTransformerHACK.DummyModulesToAdd -> {
        val dummyModules = dummyModulesResult.dummyModules
        val dummyModuleDependencies =
          if (BazelFeatureFlags.addDummyModuleDependencies) {
            dummyModules.map { IntermediateModuleDependency(it.genericModuleInfo.name) }
          } else {
            emptyList()
          }
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
      is JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots -> {
        val javaModuleWithMergedSourceRoots =
          javaModule.copy(
            sourceRoots = dummyModulesResult.mergedSourceRoots,
            resourceRoots = dummyModulesResult.mergedResourceRoots ?: javaModule.resourceRoots,
          )
        listOf(javaModuleWithMergedSourceRoots)
      }
    }
  }

  private fun resolveCompiledClassesPathForJVMLanguage(input: ModuleDetails): Path? {
    val target = input.target.id
    val targetDir =
      project.rootDir
        .toNioPath()
        .resolve("bazel-bin")
        .resolve(target.packagePath.toString())
    val targetData = input.target.data
    return when (targetData) {
      is JvmBuildTarget, is KotlinBuildTarget, is ScalaBuildTarget -> {
        if (input.target.kind.ruleType == RuleType.LIBRARY) {
          targetDir.resolve("lib${target.targetName}.jar")
        } else {
          targetDir.resolve("${target.targetName}.jar")
        }
      }
      else -> null
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
      )

    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails)
  }

  private fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      path = inputEntity.target.baseDirectory,
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
    val version = scalaBuildTarget?.scalaVersion ?: return null
    return ScalaAddendum(
      scalaVersion = version,
      scalacOptions = scalaBuildTarget.scalacOptions,
      sdkJars = scalaBuildTarget.sdkJars,
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
        manifest = manifest,
        manifestOverrides = manifestOverrides,
        resourceDirectories = resourceDirectories,
        resourceJavaPackage = resourceJavaPackage,
        assetsDirectories = assetsDirectories,
        apk = apk,
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

fun String.projectNameToJdkName(javaHomeUri: Path): String =
  projectNameToBaseJdkName() + "-" + StringUtils.md5Hash(javaHomeUri.toString(), 5)

fun Path.androidJarToAndroidSdkName(): String = "android-sdk-" + StringUtils.md5Hash(this.toString(), 5)
