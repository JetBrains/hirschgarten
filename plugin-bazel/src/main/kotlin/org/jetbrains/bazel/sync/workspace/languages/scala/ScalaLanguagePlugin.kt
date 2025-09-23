package org.jetbrains.bazel.sync.workspace.languages.scala

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.isInternalTarget
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bazel.sync.workspace.model.Library
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import java.nio.file.Path

class ScalaLanguagePlugin(
  private val javaLanguagePlugin: JavaLanguagePlugin,
  private val bazelPathsResolver: BazelPathsResolver,
  private val packageResolver: JvmPackageResolver = DefaultJvmPackageResolver(),
) : LanguagePlugin<ScalaBuildTarget>,
  JVMPackagePrefixResolver {
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

  // Project-level libraries: Scala SDK jars and ScalaTest jars
  override fun collectProjectLevelLibraries(targets: Sequence<BspTargetInfo.TargetInfo>): Map<Label, Library> {
    val sdkJars: Set<Path> = scalaSdks.values.flatMap { it.compilerJars }.toSet()
    val scalatestJars: Set<Path> = scalaTestJars.values.flatten().toSet()
    val allJars = (sdkJars + scalatestJars)
    if (allJars.isEmpty()) return emptyMap()
    return allJars.associate { jar ->
      val label = Label.synthetic(jar.fileName.toString())
      label to Library(
        label = label,
        outputs = setOf(jar),
        sources = emptySet(),
        dependencies = emptyList(),
      )
    }
  }

  // Per-target libraries: map each Scala target to appropriate SDK and ScalaTest libraries
  override fun collectPerTargetLibraries(targets: Sequence<BspTargetInfo.TargetInfo>): Map<Label, List<Library>> =
    targets
      .filter { it.hasScalaTargetInfo() }
      .associate { targetInfo ->
        val targetLabel = targetInfo.label()
        val sdkJars = scalaSdks[targetLabel]?.compilerJars.orEmpty()
        val testJars = scalaTestJars[targetLabel].orEmpty()
        val libs = (sdkJars + testJars).map { jar ->
          val libLabel = Label.synthetic(jar.fileName.toString())
          Library(
            label = libLabel,
            outputs = setOf(jar),
            sources = emptySet(),
            dependencies = emptyList(),
          )
        }
        targetLabel to libs
      }

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.SCALA)

  override fun supportsTarget(target: BspTargetInfo.TargetInfo): Boolean = target.hasScalaTargetInfo() || target.kind in setOf(
    "scala_library", "scala_binary", "scala_test"
  )

  override fun isWorkspaceTarget(
    target: BspTargetInfo.TargetInfo,
    repoMapping: RepoMapping,
    featureFlags: FeatureFlags,
  ): Boolean {
    val internal = isInternalTarget(target.label().assumeResolved(), repoMapping)
    return internal && supportsTarget(target)
  }

  override fun collectResources(targetInfo: BspTargetInfo.TargetInfo): Sequence<Path> =
    collectResourcesWithCheck(targetInfo, bazelPathsResolver) { it.hasScalaTargetInfo() }

  override fun resolveJvmPackagePrefix(source: Path): String? = packageResolver.calculateJvmPackagePrefix(source, true)
}
