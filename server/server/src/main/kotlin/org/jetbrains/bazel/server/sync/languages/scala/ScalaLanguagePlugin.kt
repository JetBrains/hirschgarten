package org.jetbrains.bazel.server.sync.languages.scala

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.server.model.BspMappings
import org.jetbrains.bazel.server.model.Language
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.model.Tag
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.JVMLanguagePluginParser
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bazel.server.sync.languages.SourceRootAndData
import org.jetbrains.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.java.JavaModule
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.ScalaMainClass
import org.jetbrains.bsp.protocol.ScalaMainClassesItem
import org.jetbrains.bsp.protocol.ScalaPlatform
import org.jetbrains.bsp.protocol.ScalaTestClassesItem
import java.net.URI
import java.nio.file.Path

class ScalaLanguagePlugin(private val javaLanguagePlugin: JavaLanguagePlugin, private val bazelPathsResolver: BazelPathsResolver) :
  LanguagePlugin<ScalaModule>() {
  var scalaSdks: Map<Label, ScalaSdk> = emptyMap()
  var scalaTestJars: Map<Label, Set<URI>> = emptyMap()

  override fun prepareSync(targets: Sequence<BspTargetInfo.TargetInfo>) {
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
              .map(bazelPathsResolver::resolveUri)
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

  override fun dependencySources(targetInfo: BspTargetInfo.TargetInfo, dependencyGraph: DependencyGraph): Set<URI> =
    javaLanguagePlugin.dependencySources(targetInfo, dependencyGraph)

  override fun applyModuleData(moduleData: ScalaModule, buildTarget: BuildTarget) {
    val scalaBuildTarget =
      with(moduleData.sdk) {
        ScalaBuildTarget(
          organization,
          version,
          binaryVersion,
          ScalaPlatform.JVM,
          compilerJars.map { it.toString() }.toList(),
          jvmBuildTarget = moduleData.javaModule?.let(javaLanguagePlugin::toJvmBuildTarget),
        )
      }
    buildTarget.data = scalaBuildTarget
  }

  override fun calculateSourceRootAndAdditionalData(source: Path): SourceRootAndData =
    JVMLanguagePluginParser.calculateJVMSourceRootAndAdditionalData(source, true)

  fun toScalaTestClassesItem(module: Module): ScalaTestClassesItem? =
    if (!module.tags.contains(Tag.TEST) ||
      !module.languages
        .contains(Language.SCALA)
    ) {
      null
    } else {
      withScalaAndJavaModules(module) { _, javaModule: JavaModule ->
        val mainClasses: List<String> = listOfNotNull(javaModule.mainClass)
        val id = BspMappings.toBspId(module)
        ScalaTestClassesItem(id, classes = mainClasses)
      }
    }

  fun toScalaMainClassesItem(module: Module): ScalaMainClassesItem? =
    if (!module.tags.contains(Tag.APPLICATION) ||
      !module.languages
        .contains(Language.SCALA)
    ) {
      null
    } else {
      withScalaAndJavaModulesOpt(module) { _, javaModule: JavaModule ->
        javaModule.mainClass?.let { mainClass: String ->
          val id = BspMappings.toBspId(module)
          val args = javaModule.args
          val jvmOpts = javaModule.jvmOps
          val scalaMainClass = ScalaMainClass(mainClass, args.toList(), jvmOpts.toList())
          val mainClasses = listOf(scalaMainClass)
          ScalaMainClassesItem(id, mainClasses)
        }
      }
    }

  private fun <T> withScalaAndJavaModules(module: Module, f: (ScalaModule, JavaModule) -> T): T? =
    getScalaAndJavaModules(module)?.let { (a, b) -> f(a, b) }

  private fun <T> withScalaAndJavaModulesOpt(module: Module, f: (ScalaModule, JavaModule) -> T?): T? =
    getScalaAndJavaModules(module)?.let { (a, b) -> f(a, b) }

  private fun getScalaAndJavaModules(module: Module): Pair<ScalaModule, JavaModule>? =
    (module.languageData as? ScalaModule)?.let { scala ->
      scala.javaModule?.let { Pair(scala, it) }
    }
}
