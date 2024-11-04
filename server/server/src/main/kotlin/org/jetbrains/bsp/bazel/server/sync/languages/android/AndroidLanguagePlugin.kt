package org.jetbrains.bsp.bazel.server.sync.languages.android

import ch.epfl.scala.bsp4j.BuildTarget
import org.jetbrains.bsp.bazel.info.BspTargetInfo
import org.jetbrains.bsp.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.SourceRootAndData
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.kotlin.KotlinModule
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.AndroidBuildTarget
import org.jetbrains.bsp.protocol.AndroidTargetType
import java.net.URI
import java.nio.file.Path

class AndroidLanguagePlugin(
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val javaLanguagePlugin: JavaLanguagePlugin,
  private val kotlinLanguagePlugin: KotlinLanguagePlugin,
  private val bazelPathsResolver: BazelPathsResolver,
) : LanguagePlugin<AndroidModule>() {
  private var androidMinSdkOverride: Int? = null

  override fun prepareSync(targets: Sequence<BspTargetInfo.TargetInfo>) {
    androidMinSdkOverride = workspaceContextProvider.currentWorkspaceContext().androidMinSdkSpec.value
  }

  override fun applyModuleData(moduleData: AndroidModule, buildTarget: BuildTarget) {
    val androidBuildTarget =
      with(moduleData) {
        AndroidBuildTarget(
          androidJar = androidJar.toString(),
          androidTargetType = androidTargetType,
          manifest = manifest?.toString(),
          resourceDirectories = resourceDirectories.map { it.toString() },
          resourceJavaPackage = resourceJavaPackage,
          assetsDirectories = assetsDirectories.map { it.toString() },
          androidMinSdkOverride = androidMinSdkOverride,
        )
      }
    moduleData.javaModule?.let { javaLanguagePlugin.toJvmBuildTarget(it) }?.let {
      androidBuildTarget.jvmBuildTarget = it
    }
    moduleData.kotlinModule?.let { kotlinLanguagePlugin.toKotlinBuildTarget(it) }?.let {
      androidBuildTarget.kotlinBuildTarget = it
    }

    buildTarget.dataKind = "android"
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
    val resourceDirectories = bazelPathsResolver.resolveUris(androidTargetInfo.resourceDirectoriesList)
    val resourceJavaPackage = androidTargetInfo.resourceJavaPackage.takeIf { it.isNotEmpty() }
    val assetsDirectories = bazelPathsResolver.resolveUris(androidTargetInfo.assetsDirectoriesList)

    val kotlinModule: KotlinModule? = kotlinLanguagePlugin.resolveModule(targetInfo)

    return AndroidModule(
      androidJar = androidJar,
      androidTargetType = getAndroidTargetType(targetInfo),
      manifest = manifest,
      resourceDirectories = resourceDirectories,
      resourceJavaPackage = resourceJavaPackage,
      assetsDirectories = assetsDirectories,
      androidMinSdkOverride = androidMinSdkOverride,
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

  override fun calculateSourceRootAndAdditionalData(source: Path): SourceRootAndData =
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
