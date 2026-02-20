package org.jetbrains.bazel.sync.workspace.languages.scala

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path

class ScalaLanguagePlugin(
  private val javaLanguagePlugin: JavaLanguagePlugin,
  private val bazelPathsResolver: BazelPathsResolver,
  private val packageResolver: JvmPackageResolver = DefaultJvmPackageResolver(),
) : LanguagePlugin<ScalaBuildTarget>,
  JVMPackagePrefixResolver {
  var scalaSdks: Map<Label, ScalaSdk> = emptyMap()
  var scalaTestJars: Map<Label, Set<Path>> = emptyMap()

  override fun prepareSync(project: Project, targets: Map<Label, BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {
    scalaSdks =
      targets.values.asSequence()
        .associateBy(
          { it.label() },
          ScalaSdkResolver(bazelPathsResolver)::resolveSdk,
        ).filterValuesNotNull()
    scalaTestJars =
      targets.values.asSequence()
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

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): ScalaBuildTarget? {
    if (!target.hasScalaTargetInfo()) {
      return null
    }
    val sdk = scalaSdks[target.label()] ?: return null
    return ScalaBuildTarget(
      scalaVersion = sdk.version,
      sdkJars = sdk.compilerJars,
      jvmBuildTarget = javaLanguagePlugin.createBuildTargetData(context, target),
      scalacOptions = target.scalaTargetInfo.scalacOptsList,
    )
  }

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.SCALA)

  override fun resolveJvmPackagePrefix(source: Path): String? = packageResolver.calculateJvmPackagePrefix(source, true)

  override fun transformSources(sources: List<SourceItem>): List<SourceItem> = javaLanguagePlugin.transformSources(sources)

}
