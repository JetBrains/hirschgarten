package org.jetbrains.bazel.server.sync.languages.scala

import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
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
  var scalaSdks: Map<CanonicalLabel, ScalaSdk> = emptyMap()
  var scalaTestJars: Map<CanonicalLabel, Set<Path>> = emptyMap()

  override fun prepareSync(targets: Sequence<TargetInfo>, workspaceContext: WorkspaceContext) {
    scalaSdks =
      targets
        .associateBy(
          { it.id },
          ScalaSdkResolver(bazelPathsResolver)::resolveSdk,
        ).filterValuesNotNull()
    scalaTestJars =
      targets
        .filter { it.scalaTargetInfo != null }
        .associateBy(
          { it.id },
          {
            it.scalaTargetInfo!!.scalatestClasspath
              .map(bazelPathsResolver::resolve)
              .toSet()
          },
        )
  }

  private fun <K, V> Map<K, V?>.filterValuesNotNull(): Map<K, V> = filterValues { it != null }.mapValues { it.value!! }

  override fun resolveModule(targetInfo: TargetInfo): ScalaModule? {
    val scalaTargetInfo = targetInfo.scalaTargetInfo ?: return null
    val sdk = scalaSdks[targetInfo.id] ?: return null
    val scalacOpts = scalaTargetInfo.scalacOpts
    return ScalaModule(sdk, scalacOpts, javaLanguagePlugin.resolveModule(targetInfo))
  }

  override fun dependencySources(targetInfo: TargetInfo, dependencyGraph: DependencyGraph): Set<Path> =
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
