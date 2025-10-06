package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.JvmOutputs
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class JavaLanguagePluginTest {
  private fun resolver(tmp: Path? = null): BazelPathsResolver = BazelPathsResolverMock.create(tmp ?: Path.of(""))

  private fun javaPlugin(resolver: BazelPathsResolver): JavaLanguagePlugin =
    JavaLanguagePlugin(resolver, JdkResolver(resolver, JdkVersionResolver()), DefaultJvmPackageResolver(), org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver())

  private fun fileLocation(path: String): FileLocation = FileLocation.newBuilder().setRelativePath(path).build()

  private fun outputs(binary: String, sources: String? = null, iface: String? = null): JvmOutputs =
    JvmOutputs.newBuilder()
      .addBinaryJars(fileLocation(binary))
      .apply { if (sources != null) addSourceJars(fileLocation(sources)) }
      .apply { if (iface != null) addInterfaceJars(fileLocation(iface)) }
      .build()

  private fun targetWithJvm(kind: String, id: String, jars: List<JvmOutputs>, generated: List<JvmOutputs>, apiGenerating: Boolean = false): TargetInfo =
    TargetInfo.newBuilder()
      .setId(id)
      .setKind(kind)
      .setJvmTargetInfo(
        JvmTargetInfo.newBuilder()
          .addAllJars(jars)
          .addAllGeneratedJars(generated)
          .setHasApiGeneratingPlugins(apiGenerating)
          .build()
      )
      .build()

  @Test
  fun `collect per-target libraries includes generated jars and output jars`() {
    val pathsResolver = resolver()
    val plugin = javaPlugin(pathsResolver)

    val binJar = "bazel-bin/app/libapp.jar"
    val srcJar = "bazel-bin/app/libapp-src.jar"
    val genBinJar = "bazel-bin/app/libapp_gen.jar"
    val genSrcJar = "bazel-bin/app/libapp_gen-src.jar"

    val jvmJars = listOf(outputs(binJar, srcJar))
    val jvmGen = listOf(outputs(genBinJar, genSrcJar))

    val t = targetWithJvm(kind = "java_library", id = "//app:lib", jars = jvmJars, generated = jvmGen, apiGenerating = true)

    val perTarget = plugin.collectPerTargetLibraries(sequenceOf(t))
    val libs = perTarget[Label.parse(t.id)] ?: emptyList()

    // Expect: one library for generated jars + one for output jars
    assertTrue(libs.any { lib -> lib.outputs.any { it.fileName.toString() == "libapp_gen.jar" } })
    assertTrue(libs.any { lib -> lib.outputs.any { it.fileName.toString() == "libapp.jar" } })
  }
}
