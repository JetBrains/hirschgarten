package org.jetbrains.bazel.server.sync.languages.android

import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.kotlin.KotlinModule
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.AndroidBuildTarget
import org.jetbrains.bsp.protocol.AndroidTargetType
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

class AndroidLanguagePlugin(
  private val javaLanguagePlugin: JavaLanguagePlugin,
  private val kotlinLanguagePlugin: KotlinLanguagePlugin,
  private val bazelPathsResolver: BazelPathsResolver,
) : LanguagePlugin<AndroidModule>() {
  private var androidMinSdkOverride: Int? = null

  override fun prepareSync(targets: Sequence<TargetInfo>, workspaceContext: WorkspaceContext) {
    androidMinSdkOverride = workspaceContext.androidMinSdkSpec.value
  }

  override fun applyModuleData(moduleData: AndroidModule, buildTarget: RawBuildTarget) {
    val androidBuildTarget =
      with(moduleData) {
        AndroidBuildTarget(
          androidJar = androidJar,
          androidTargetType = androidTargetType,
          manifest = manifest,
          manifestOverrides = manifestOverrides,
          resourceDirectories = resourceDirectories,
          resourceJavaPackage = resourceJavaPackage,
          assetsDirectories = assetsDirectories,
          apk = apk,
        )
      }
    moduleData.javaModule?.let { javaLanguagePlugin.toJvmBuildTarget(it) }?.let {
      androidBuildTarget.jvmBuildTarget = it
    }
    moduleData.kotlinModule?.let { kotlinLanguagePlugin.toKotlinBuildTarget(it) }?.let {
      androidBuildTarget.kotlinBuildTarget = it
    }

    buildTarget.data = androidBuildTarget
  }

  override fun resolveModule(targetInfo: TargetInfo): AndroidModule? {
    val androidTargetInfo = targetInfo.androidTargetInfo ?: return null
    val androidJar = bazelPathsResolver.resolve(androidTargetInfo.androidJar)
    val manifest =
        bazelPathsResolver.resolve(androidTargetInfo.manifest)
    val manifestOverrides =
      androidTargetInfo.manifestOverrides.run {
        androidMinSdkOverride?.let { androidMinSdkOverride ->
          this + ("minSdkVersion" to androidMinSdkOverride.toString())
        } ?: this
      }
    val resourceDirectories = bazelPathsResolver.resolvePaths(androidTargetInfo.resourceDirectories)
    val resourceJavaPackage = androidTargetInfo.resourceJavaPackage.takeIf { it.isNotEmpty() }
    val assetsDirectories = bazelPathsResolver.resolvePaths(androidTargetInfo.assetsDirectories)
    val apk = bazelPathsResolver.resolve(androidTargetInfo.apk)

    val kotlinModule: KotlinModule? = kotlinLanguagePlugin.resolveModule(targetInfo)

    return AndroidModule(
      androidJar = androidJar,
      androidTargetType = getAndroidTargetType(targetInfo),
      manifest = manifest,
      manifestOverrides = manifestOverrides,
      resourceDirectories = resourceDirectories,
      resourceJavaPackage = resourceJavaPackage,
      assetsDirectories = assetsDirectories,
      apk = apk,
      javaModule = kotlinModule?.javaModule ?: javaLanguagePlugin.resolveModule(targetInfo),
      kotlinModule = kotlinModule,
      correspondingKotlinTarget = null,
    )
  }

  private fun getAndroidTargetType(targetInfo: TargetInfo): AndroidTargetType =
    when (targetInfo.kind) {
      "android_binary" -> AndroidTargetType.APP
      "android_library" -> AndroidTargetType.LIBRARY
      "android_local_test", "android_instrumentation_test" -> AndroidTargetType.TEST
      else -> AndroidTargetType.LIBRARY
    }

  override fun dependencySources(targetInfo: TargetInfo, dependencyGraph: DependencyGraph): Set<Path> =
    javaLanguagePlugin.dependencySources(targetInfo, dependencyGraph)

  override fun calculateJvmPackagePrefix(source: Path): String? = javaLanguagePlugin.calculateJvmPackagePrefix(source)

  override fun resolveAdditionalResources(targetInfo: TargetInfo): Set<Path> {
    val androidTargetInfo = targetInfo.androidTargetInfo ?: return emptySet()

    return bazelPathsResolver
      .resolvePaths(listOf(androidTargetInfo.manifest) + androidTargetInfo.resourceDirectories)
      .toSet()
  }
}
