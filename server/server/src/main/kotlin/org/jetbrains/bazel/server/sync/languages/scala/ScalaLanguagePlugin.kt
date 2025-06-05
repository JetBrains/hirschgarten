package org.jetbrains.bazel.server.sync.languages.scala

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.JVMLanguagePluginParser
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bazel.server.sync.languages.java.JavaLanguagePlugin
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
          jars = compilerJars.toList(),
          jvmBuildTarget = moduleData.javaModule?.let(javaLanguagePlugin::toJvmBuildTarget),
        )
      }
    buildTarget.data = scalaBuildTarget
  }

  override fun calculateJvmPackagePrefix(source: Path): String? =
    JVMLanguagePluginParser.calculateJVMSourceRootAndAdditionalData(source, true)
}
