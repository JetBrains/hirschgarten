package org.jetbrains.bazel.sync.workspace.languages.java

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.LifecycleAware
import org.jetbrains.bazel.sync.workspace.languages.java.JavaPluginData.JavaRunTargetInfo
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bazel.utils.store.KVStore
import org.jetbrains.bazel.utils.store.codec.ProtoStoreCodec
import org.jetbrains.bazel.utils.store.mvstore.MVStoreKVStore
import org.jetbrains.bazel.utils.store.mvstore.MVStoreUtils
import org.jetbrains.bazel.utils.store.utils.toLabelStore
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.JvmBuildTarget
import java.nio.file.Path

class JavaLanguagePlugin(
  private val project: Project,
  private val bazelPathsResolver: BazelPathsResolver,
  private val jdkResolver: JdkResolver,
  private val packageResolver: JvmPackageResolver,
  private val environmentProvider: EnvironmentProvider = EnvironmentProvider.getInstance(),
) : LanguagePlugin<JvmBuildTarget>, LifecycleAware, JVMPackagePrefixResolver {
  private val mvStore = MVStoreUtils.openStoreForProject(project, "java-langauge-plugin")
  private var jdk: Jdk? = null

  val runTargetInfoStore: KVStore<Label, JavaPluginData.JavaRunTargetInfo> = MVStoreKVStore.createHashedFromCodecs(
    store = mvStore,
    name = "run-target-info",
    valueCodec = ProtoStoreCodec(JavaRunTargetInfo::parseFrom),
  ).toLabelStore()

  override fun prepareSync(targets: Sequence<TargetInfo>, workspaceContext: WorkspaceContext) {
    val ideJavaHomeOverride = workspaceContext.ideJavaHomeOverrideSpec.value
    jdk = ideJavaHomeOverride?.let { Jdk(version = "ideJavaHomeOverride", javaHome = it) } ?: jdkResolver.resolve(targets)
    runTargetInfoStore.clear()
  }

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: TargetInfo): JvmBuildTarget? {
    if (!target.hasJvmTargetInfo()) {
      return null
    }
    val jvmTarget = target.jvmTargetInfo
    val binaryOutputs = jvmTarget.jarsList.flatMap { it.binaryJarsList }.map(bazelPathsResolver::resolve)

    val jdk = jdk ?: return null
    val javaHome = jdk.javaHome ?: return null

    return JvmBuildTarget(
      javaVersion = javaVersionFromJavacOpts(jvmTarget.javacOptsList) ?: jdk.version,
      javaHome = javaHome,
      javacOpts = jvmTarget.javacOptsList,
      binaryOutputs = binaryOutputs,
    )
  }

  override suspend fun processTarget(context: LanguagePluginContext, target: TargetInfo) {
    if (!target.hasJvmTargetInfo()) {
      return
    }
    val jvmTarget = target.jvmTargetInfo
    val mainClass = getMainClass(jvmTarget)
    val environmentVariables =
      context.target.envMap + context.target.envInheritList.associateWith { environmentProvider.getValue(it) ?: "" }

    val runTargetInfo = JavaRunTargetInfo.newBuilder()
      .putAllEnvVariables(environmentVariables)
      .addAllJvmArgs(jvmTarget.jvmFlagsList)
      .addAllProgramArgs(jvmTarget.argsList)
      .also { builder -> mainClass?.let(builder::setMainClass) }
      .build()
    runTargetInfoStore.put(target.label(), runTargetInfo)
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

  override fun onSave() {
    mvStore.tryCommit()
  }
}
