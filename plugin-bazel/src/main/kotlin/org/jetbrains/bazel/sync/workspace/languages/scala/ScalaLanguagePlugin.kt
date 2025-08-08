package org.jetbrains.bazel.sync.workspace.languages.scala

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.JVMLanguagePluginParser
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import java.nio.file.Path

class ScalaLanguagePlugin(private val javaLanguagePlugin: JavaLanguagePlugin, private val bazelPathsResolver: BazelPathsResolver) :
  LanguagePlugin<ScalaModule>() {
  var scalaSdks: Map<Label, ScalaSdk> = emptyMap()
  var scalaTestJars: Map<Label, Set<Path>> = emptyMap()

  override fun prepareSync(targets: Sequence<BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {
    scalaSdks =
      targets
        .associateBy(
          { it.label() },
          ScalaSdkResolver(bazelPathsResolver)::resolveSdk,
        ).filterValuesNotNull()
    scalaTestJars =
      targets
        .filter { it.hasScalaTargetInfo() }
        .associateBy(
          { it.label() },
          {
            it.scalaTargetInfo.scalatestClasspathList
              .map(bazelPathsResolver::resolve)
              .toSet()
          },
        )
  }

  private fun <K, V> Map<K, V?>.filterValuesNotNull(): Map<K, V> = filterValues { it != null }.mapValues { it.value!! }

  override fun resolveModule(targetInfo: BspTargetInfo.TargetInfo): ScalaModule? {
    if (!targetInfo.hasScalaTargetInfo()) {
      return null
    }
    val scalaTargetInfo = targetInfo.scalaTargetInfo
    val sdk = scalaSdks[targetInfo.label()] ?: return null
    val scalacOpts = scalaTargetInfo.scalacOptsList
    return ScalaModule(sdk, scalacOpts, javaLanguagePlugin.resolveModule(targetInfo))
  }

  override fun dependencySources(targetInfo: BspTargetInfo.TargetInfo, dependencyGraph: DependencyGraph): Set<Path> =
    javaLanguagePlugin.dependencySources(targetInfo, dependencyGraph)

  override fun applyModuleData(moduleData: ScalaModule, buildTarget: RawBuildTarget) {
    val scalaBuildTarget =
      with(moduleData.sdk) {
        ScalaBuildTarget(
          scalaVersion = version,
          sdkJars = compilerJars.toList(),
          jvmBuildTarget = moduleData.javaModule?.let(javaLanguagePlugin::toJvmBuildTarget),
          scalacOptions = moduleData.scalacOpts,
        )
      }
    buildTarget.data = scalaBuildTarget
  }

  override fun calculateJvmPackagePrefix(source: Path): String? =
    JVMLanguagePluginParser.calculateJVMSourceRootAndAdditionalData(source, true)
}
