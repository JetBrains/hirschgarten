package org.jetbrains.bazel.server.sync.languages.android

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bazel.server.sync.languages.SourceRootAndData
import org.jetbrains.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.kotlin.KotlinModule
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.AndroidBuildTarget
import org.jetbrains.bsp.protocol.AndroidTargetType
import org.jetbrains.bsp.protocol.BuildTarget
import java.net.URI
import java.nio.file.Path

class AndroidLanguagePlugin(
  private val javaLanguagePlugin: JavaLanguagePlugin,
  private val kotlinLanguagePlugin: KotlinLanguagePlugin,
  private val bazelPathsResolver: BazelPathsResolver,
) : LanguagePlugin<AndroidModule>() {
  private var androidMinSdkOverride: Int? = null

  override fun prepareSync(targets: Sequence<BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {
    androidMinSdkOverride = workspaceContext.androidMinSdkSpec.value
  }

  override fun applyModuleData(moduleData: AndroidModule, buildTarget: BuildTarget) {
    val androidBuildTarget =
      with(moduleData) {
        AndroidBuildTarget(
          androidJar = androidJar.toString(),
          androidTargetType = androidTargetType,
          manifest = manifest?.toString(),
          manifestOverrides = manifestOverrides,
          resourceDirectories = resourceDirectories.map { it.toString() },
          resourceJavaPackage = resourceJavaPackage,
          assetsDirectories = assetsDirectories.map { it.toString() },
          apk = apk?.toString(),
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

  override fun resolveModule(targetInfo: BspTargetInfo.TargetInfo): AndroidModule? {
    if (!targetInfo.hasAndroidTargetInfo()) return null

    val androidTargetInfo = targetInfo.androidTargetInfo
    val androidJar = bazelPathsResolver.resolveUri(androidTargetInfo.androidJar)
    val manifest =
      if (androidTargetInfo.hasManifest()) {
        bazelPathsResolver.resolveUri(androidTargetInfo.manifest)
      } else {
        null
      }
    val manifestOverrides =
      androidTargetInfo.manifestOverridesMap.run {
        androidMinSdkOverride?.let { androidMinSdkOverride ->
          this + ("minSdkVersion" to androidMinSdkOverride.toString())
        } ?: this
      }
    val resourceDirectories = bazelPathsResolver.resolveUris(androidTargetInfo.resourceDirectoriesList)
    val resourceJavaPackage = androidTargetInfo.resourceJavaPackage.takeIf { it.isNotEmpty() }
    val assetsDirectories = bazelPathsResolver.resolveUris(androidTargetInfo.assetsDirectoriesList)
    val apk =
      if (androidTargetInfo.hasApk()) {
        bazelPathsResolver.resolveUri(androidTargetInfo.apk)
      } else {
        null
      }

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

  private fun getAndroidTargetType(targetInfo: BspTargetInfo.TargetInfo): AndroidTargetType =
    when (targetInfo.kind) {
      "android_binary" -> AndroidTargetType.APP
      "android_library" -> AndroidTargetType.LIBRARY
      "android_local_test", "android_instrumentation_test" -> AndroidTargetType.TEST
      else -> AndroidTargetType.LIBRARY
    }

  override fun dependencySources(targetInfo: BspTargetInfo.TargetInfo, dependencyGraph: DependencyGraph): Set<URI> =
    javaLanguagePlugin.dependencySources(targetInfo, dependencyGraph)

  override fun calculateSourceRootAndAdditionalData(source: Path): SourceRootAndData? =
    javaLanguagePlugin.calculateSourceRootAndAdditionalData(source)

  override fun resolveAdditionalResources(targetInfo: BspTargetInfo.TargetInfo): Set<URI> {
    if (!targetInfo.hasAndroidTargetInfo()) return emptySet()
    val androidTargetInfo = targetInfo.androidTargetInfo
    if (!androidTargetInfo.hasManifest()) return emptySet()

    return bazelPathsResolver
      .resolveUris(listOf(targetInfo.androidTargetInfo.manifest) + targetInfo.androidTargetInfo.resourceDirectoriesList)
      .toSet()
  }
}
