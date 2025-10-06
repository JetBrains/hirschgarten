package org.jetbrains.bazel.sync.workspace.languages.kotlin

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.KotlinTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.KotlincPluginInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JdkResolver
import org.jetbrains.bazel.sync.workspace.languages.java.JdkVersionResolver
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class KotlinLanguagePluginTest {
  private fun resolver(tmp: Path? = null): BazelPathsResolver =
    BazelPathsResolverMock.create(tmp ?: Path.of("") )

  private fun kotlinPlugin(resolver: BazelPathsResolver): KotlinLanguagePlugin {
    val javaPlugin = JavaLanguagePlugin(resolver, JdkResolver(resolver, JdkVersionResolver()), DefaultJvmPackageResolver(), org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver())
    return KotlinLanguagePlugin(javaPlugin, resolver)
  }

  private fun stdlib(flPath: String): FileLocation =
    FileLocation.newBuilder().setRelativePath(flPath).build()

  private fun pluginJar(flPath: String): FileLocation =
    FileLocation.newBuilder().setRelativePath(flPath).build()

  private fun kotlinTarget(stdlibs: List<FileLocation>, pluginJars: List<FileLocation>): KotlinTargetInfo =
    KotlinTargetInfo
      .newBuilder()
      .setLanguageVersion("1.9")
      .setApiVersion("1.9")
      .addAllStdlibs(stdlibs)
      .addKotlincPluginInfos(
        KotlincPluginInfo.newBuilder().addAllPluginJars(pluginJars).build()
      )
      .build()

  private fun target(id: String, kind: String, kt: KotlinTargetInfo): TargetInfo =
    TargetInfo.newBuilder()
      .setId(id)
      .setKind(kind)
      .setKotlinTargetInfo(kt)
      .build()

  @Test
  fun `collect Kotlin stdlibs as project-level bundle and per-target plugin jars`() {
    val pathsResolver = resolver()
    val plugin = kotlinPlugin(pathsResolver)

    val stdlibJarPath = "external/maven/v1/https/maven/org/jetbrains/kotlin/kotlin-stdlib-1.9.0.jar"
    val pluginJarPath = "bazel-bin/plugins/kotlin/my-plugin.jar"

    val ktInfo1 = kotlinTarget(
      stdlibs = listOf(stdlib(stdlibJarPath)),
      pluginJars = listOf(pluginJar(pluginJarPath))
    )
    val ktInfo2 = kotlinTarget(
      stdlibs = listOf(stdlib(stdlibJarPath)),
      pluginJars = emptyList()
    )

    val t1 = target("//app:lib", "kt_jvm_library", ktInfo1)
    val t2 = target("//core:util", "kt_jvm_library", ktInfo2)

    // Project-level stdlib bundle
    val projectLibs = plugin.collectProjectLevelLibraries(sequenceOf(t1, t2))
    assertTrue(projectLibs.isNotEmpty())
    val stdlibBundle = projectLibs.values.firstOrNull()
    assertNotNull(stdlibBundle)
    stdlibBundle!!
    // should contain the stdlib jar and inferred -sources.jar
    val stdlibOutputsNames = stdlibBundle.outputs.map { it.fileName.toString() }.toSet()
    assertTrue(stdlibOutputsNames.any { it.contains("kotlin-stdlib") && it.endsWith(".jar") })
    val inferredSourcesNames = stdlibBundle.sources.map { it.fileName.toString() }.toSet()
    assertTrue(inferredSourcesNames.any { it.contains("kotlin-stdlib") && it.endsWith("-sources.jar") })

    // Per-target mapping should include the stdlib bundle and plugin jars for t1
    val perTarget = plugin.collectPerTargetLibraries(sequenceOf(t1, t2))
    assertTrue(perTarget.containsKey(Label.parse(t1.id)))
    val libsForT1 = perTarget[Label.parse(t1.id)]!!
    // stdlib bundle included
    assertTrue(libsForT1.any { it.label == stdlibBundle.label })
    // plugin jar library included
    val pluginLibOutputs = libsForT1.flatMap { it.outputs }.map { it.fileName.toString() }.toSet()
    assertTrue(pluginLibOutputs.contains("my-plugin.jar"))

    // t2 has no plugin jars but should still include the stdlib bundle
    val libsForT2 = perTarget[Label.parse(t2.id)]!!
    assertTrue(libsForT2.any { it.label == stdlibBundle.label })
    assertEquals(false, libsForT2.any { it.outputs.any { p -> p.fileName.toString() == "my-plugin.jar" } })
  }
}
