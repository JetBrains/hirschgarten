package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMLanguagePluginParser
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.JvmBuildTarget
import java.nio.file.Path

class JavaLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver, private val jdkResolver: JdkResolver) :
  LanguagePlugin<JavaModule, JvmBuildTarget> {
  private var jdk: Jdk? = null

  override fun prepareSync(targets: Sequence<TargetInfo>, workspaceContext: WorkspaceContext) {
    val ideJavaHomeOverride = workspaceContext.ideJavaHomeOverrideSpec.value
    jdk = ideJavaHomeOverride?.let { Jdk(version = "ideJavaHomeOverride", javaHome = it) } ?: jdkResolver.resolve(targets)
  }

  override fun createIntermediateModel(targetInfo: TargetInfo): JavaModule? =
    targetInfo.takeIf(TargetInfo::hasJvmTargetInfo)?.jvmTargetInfo?.run {
      if (jarsCount == 0) return@run null
      val mainOutput = bazelPathsResolver.resolve(getJars(0).getBinaryJars(0))
      val binaryOutputs = jarsList.flatMap { it.binaryJarsList }.map(bazelPathsResolver::resolve)
      val mainClass = getMainClass(this)
      val runtimeJdk = jdkResolver.resolveJdk(targetInfo)

      JavaModule(
        jdk,
        runtimeJdk,
        javacOptsList,
        jvmFlagsList,
        mainOutput,
        binaryOutputs,
        mainClass,
        argsList,
      )
    }

  override fun createBuildTargetData(context: LanguagePluginContext, ir: JavaModule): JvmBuildTarget? {
    val jdk = ir.jdk ?: return null
    val javaHome = jdk.javaHome ?: return null
    val environmentVariables =
      context.target.envMap + context.target.envInheritList.associateWith { EnvironmentProvider.getInstance().getValue(it) ?: "" }
    return JvmBuildTarget(
      javaVersion = javaVersionFromJavacOpts(ir.javacOpts) ?: jdk.version,
      javaHome = javaHome,
      javacOpts = ir.javacOpts,
      binaryOutputs = ir.binaryOutputs,
      environmentVariables = environmentVariables,
      mainClass = ir.mainClass,
      jvmArgs = ir.jvmOps,
      programArgs = ir.args,
    )
  }

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.JAVA)

  override fun calculateJvmPackagePrefix(source: Path): String? = JVMLanguagePluginParser.calculateJVMSourceRootAndAdditionalData(source)

  private fun getMainClass(jvmTargetInfo: JvmTargetInfo): String? = jvmTargetInfo.mainClass.takeUnless { jvmTargetInfo.mainClass.isBlank() }

  private fun javaVersionFromJavacOpts(javacOpts: List<String>): String? =
    javacOpts.firstNotNullOfOrNull {
      val flagName = it.substringBefore(' ')
      val argument = it.substringAfter(' ')
      if (flagName == "-target" || flagName == "--target" || flagName == "--release") argument else null
    }
}
