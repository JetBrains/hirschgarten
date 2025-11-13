package org.jetbrains.bazel.sync.workspace.languages.java

import com.intellij.util.EnvironmentUtil
import org.jetbrains.bazel.commons.BazelPathsResolver
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
) : LanguagePlugin<JvmBuildTarget>,
  JVMPackagePrefixResolver {
  private var jdk: Jdk? = null

  override fun prepareSync(targets: Sequence<TargetInfo>, workspaceContext: WorkspaceContext) {
    val ideJavaHomeOverride = workspaceContext.ideJavaHomeOverride
    jdk = ideJavaHomeOverride?.let { Jdk(javaHome = it) } ?: jdkResolver.resolve(targets)
  }

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: TargetInfo): JvmBuildTarget? {
    if (!target.hasJvmTargetInfo()) {
      return null
    }
    val jvmTarget = target.jvmTargetInfo
    val binaryOutputs = jvmTarget.jarsList.flatMap { it.binaryJarsList }.map(bazelPathsResolver::resolve)
    val mainClass = getMainClass(jvmTarget)

    val jdk = jdk ?: return null
    val javaVersion = javaVersionFromJavacOpts(jvmTarget.javacOptsList) ?: javaVersionFromToolchain(target)
    val javaHome = jdk.javaHome
    val environmentVariables =
      context.target.envMap + context.target.envInheritList.associateWith { EnvironmentUtil.getValue(it) ?: "" }
    return JvmBuildTarget(
      javaVersion = javaVersion.orEmpty(),
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

  private fun javaVersionFromToolchain(target: TargetInfo): String? = if (target.hasJavaToolchainInfo()) {
    target.javaToolchainInfo.sourceVersion
  } else {
    null
  }

  private fun javaVersionFromJavacOpts(javacOpts: List<String>): String? {
    for (i in javacOpts.indices) {
      val option = javacOpts[i]
      val flagName = option.substringBefore(' ', missingDelimiterValue = option)
      val argument = option.substringAfter(' ', missingDelimiterValue = "")
      if (flagName == "-target" || flagName == "--target" || flagName == "--release") {
        if (argument.isNotBlank()) return argument
        return javacOpts.getOrNull(i + 1)
      }
    }
    return null
  }
}
