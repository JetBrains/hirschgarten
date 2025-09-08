package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.JvmBuildTarget
import java.nio.file.Path

class JavaLanguagePlugin(
  private val bazelPathsResolver: BazelPathsResolver,
  private val jdkResolver: JdkResolver,
  private val packageResolver: JvmPackageResolver,
  private val environmentProvider: EnvironmentProvider = EnvironmentProvider.getInstance(),
) : LanguagePlugin<JvmBuildTarget>,
  JVMPackagePrefixResolver {
  private var jdk: Jdk? = null

  override fun prepareSync(targets: Sequence<TargetInfo>, workspaceContext: WorkspaceContext) {
    val ideJavaHomeOverride = workspaceContext.ideJavaHomeOverride
    jdk = ideJavaHomeOverride?.let { Jdk(version = "ideJavaHomeOverride", javaHome = it) } ?: jdkResolver.resolve(targets)
  }

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: TargetInfo): JvmBuildTarget? {
    if (!target.hasJvmTargetInfo()) {
      return null
    }
    val jvmTarget = target.jvmTargetInfo
    val binaryOutputs = jvmTarget.jarsList.flatMap { it.binaryJarsList }.map(bazelPathsResolver::resolve)
    val mainClass = getMainClass(jvmTarget)

    val jdk = jdk ?: return null
    val javaHome = jdk.javaHome ?: return null
    val environmentVariables =
      context.target.envMap + context.target.envInheritList.associateWith { environmentProvider.getValue(it) ?: "" }
    return JvmBuildTarget(
      javaVersion = javaVersionFromJavacOpts(jvmTarget.javacOptsList) ?: jdk.version,
      javaHome = javaHome,
      javacOpts = jvmTarget.javacOptsList,
      binaryOutputs = binaryOutputs,
      environmentVariables = environmentVariables,
      mainClass = mainClass,
      jvmArgs = jvmTarget.jvmFlagsList,
      programArgs = jvmTarget.argsList,
    )
  }

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.JAVA)

  override fun resolveJvmPackagePrefix(source: Path): String? = packageResolver.calculateJvmPackagePrefix(source)

  private fun getMainClass(jvmTargetInfo: JvmTargetInfo): String? = jvmTargetInfo.mainClass.takeUnless { jvmTargetInfo.mainClass.isBlank() }

  private fun javaVersionFromJavacOpts(javacOpts: List<String>): String? =
    javacOpts.firstNotNullOfOrNull {
      val flagName = it.substringBefore(' ')
      val argument = it.substringAfter(' ')
      if (flagName == "-target" || flagName == "--target" || flagName == "--release") argument else null
    }
}
