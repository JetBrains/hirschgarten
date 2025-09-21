package org.jetbrains.bazel.sync.workspace.languages.scala

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.ScalaTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.java.JdkResolver
import org.jetbrains.bazel.sync.workspace.languages.java.JdkVersionResolver
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class ScalaLanguagePluginTest {
  private fun resolver(tmp: Path? = null): BazelPathsResolver = BazelPathsResolverMock.create(tmp ?: Path.of(""))

  private fun scalaPlugin(resolver: BazelPathsResolver): ScalaLanguagePlugin {
    val java = JavaLanguagePlugin(resolver, JdkResolver(resolver, JdkVersionResolver()), DefaultJvmPackageResolver())
    return ScalaLanguagePlugin(java, resolver)
  }

  private fun fileLocation(path: String): FileLocation = FileLocation.newBuilder().setRelativePath(path).build()

  private fun scalaTarget(compilerJars: List<FileLocation>, scalatestJars: List<FileLocation>): ScalaTargetInfo =
    ScalaTargetInfo
      .newBuilder()
      .addAllCompilerClasspath(compilerJars)
      .addAllScalatestClasspath(scalatestJars)
      .addScalacOpts("-deprecation")
      .build()

  private fun target(id: String, kind: String, st: ScalaTargetInfo, resources: List<FileLocation> = emptyList()): TargetInfo =
    TargetInfo.newBuilder()
      .setId(id)
      .setKind(kind)
      .setScalaTargetInfo(st)
      .addAllResources(resources)
      .build()

  private fun workspace(): WorkspaceContext =
    WorkspaceContext(
      targets = emptyList(),
      directories = emptyList(),
      buildFlags = emptyList(),
      syncFlags = emptyList(),
      debugFlags = emptyList(),
      bazelBinary = null,
      allowManualTargetsSync = false,
      dotBazelBspDirPath = Path.of(""),
      importDepth = 1,
      enabledRules = emptyList(),
      ideJavaHomeOverride = null,
      shardSync = false,
      targetShardSize = 0,
      shardingApproach = null,
      importRunConfigurations = emptyList(),
      gazelleTarget = null,
      indexAllFilesInDirectories = false,
      pythonCodeGeneratorRuleNames = emptyList(),
      importIjars = false,
      deriveInstrumentationFilterFromTargets = false,
      indexAdditionalFilesInDirectories = emptyList(),
    )

  @Test
  fun `scala plugin exposes SDK and ScalaTest as libraries and maps per-target`() {
    val pathsResolver = resolver()
    val plugin = scalaPlugin(pathsResolver)

    val scalaLib = "external/scala/scala-library-2.13.8.jar"
    val scalaCompiler = "external/scala/scala-compiler-2.13.8.jar"
    val scalatest = "external/scala/scalatest-3.2.16.jar"

    val sInfo = scalaTarget(
      compilerJars = listOf(fileLocation(scalaLib), fileLocation(scalaCompiler)),
      scalatestJars = listOf(fileLocation(scalatest)),
    )

    val t = target("//app:scala", "scala_library", sInfo)

    // prepareSync populates sdk and scalatest state
    plugin.prepareSync(sequenceOf(t), workspace())

    // Project-level libraries should include both SDK and ScalaTest jars
    val projectLibs = plugin.collectProjectLevelLibraries(sequenceOf(t))
    val outputNames = projectLibs.values.flatMap { it.outputs }.map { it.fileName.toString() }.toSet()
    assertTrue(outputNames.contains("scala-library-2.13.8.jar"))
    assertTrue(outputNames.contains("scala-compiler-2.13.8.jar"))
    assertTrue(outputNames.contains("scalatest-3.2.16.jar"))

    // Per-target mapping should reference those jars via synthetic libraries
    val perTarget = plugin.collectPerTargetLibraries(sequenceOf(t))
    val libsForT = perTarget[Label.parse(t.id)] ?: emptyList()
    val perTargetOutputs = libsForT.flatMap { it.outputs }.map { it.fileName.toString() }.toSet()
    assertTrue(perTargetOutputs.contains("scala-library-2.13.8.jar"))
    assertTrue(perTargetOutputs.contains("scala-compiler-2.13.8.jar"))
    assertTrue(perTargetOutputs.contains("scalatest-3.2.16.jar"))
  }

  @Test
  fun `collectResources returns base resources for scala targets`() {
    val pathsResolver = resolver()
    val plugin = scalaPlugin(pathsResolver)

    val sInfo = scalaTarget(compilerJars = emptyList(), scalatestJars = emptyList())
    val resource = fileLocation("app/src/main/resources/ref.conf")
    val t = target("//app:scala", "scala_library", sInfo, resources = listOf(resource))

    // prepareSync can be invoked with empty set, not required for resources
    val resources = plugin.collectResources(t).toList()
    assertTrue(resources.any { it.fileName.toString() == "ref.conf" })
  }
}
