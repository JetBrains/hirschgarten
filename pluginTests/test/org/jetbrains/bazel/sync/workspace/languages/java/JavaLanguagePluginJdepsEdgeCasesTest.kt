package org.jetbrains.bazel.sync.workspace.languages.java

import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

/**
 * Edge case tests for JavaLanguagePlugin jdeps processing.
 */
class JavaLanguagePluginJdepsEdgeCasesTest {
  @get:Rule
  val tmpFolder = TemporaryFolder()

  private fun resolver(tmp: Path): BazelPathsResolver = BazelPathsResolverMock.create(tmp)

  private fun javaPlugin(resolver: BazelPathsResolver): JavaLanguagePlugin =
    JavaLanguagePlugin(
      resolver,
      JdkResolver(resolver, JdkVersionResolver()),
      DefaultJvmPackageResolver(),
      MavenCoordinatesResolver(),
    )

  private fun fileLocation(path: String): FileLocation =
    FileLocation.newBuilder().setRelativePath(path).build()

  private fun jvmInfo(jdepsPaths: List<String> = emptyList()): JvmTargetInfo =
    JvmTargetInfo.newBuilder().addAllJdeps(jdepsPaths.map { fileLocation(it) }).build()

  private fun target(id: String, jvmInfo: JvmTargetInfo): TargetInfo =
    TargetInfo.newBuilder()
      .setId(id)
      .setKind("java_library")
      .setJvmTargetInfo(jvmInfo)
      .build()

  @Test
  fun `collectJdepsLibraries handles empty jdeps files gracefully`() = runBlocking {
    val ws = tmpFolder.newFolder("workspace").toPath()
    val pathsResolver = resolver(ws)
    val plugin = javaPlugin(pathsResolver)

    val emptyJdepsFile = ws.resolve("bazel-bin/app/lib.jdeps")
    Files.createDirectories(emptyJdepsFile.parent)
    Files.write(emptyJdepsFile, ByteArray(0))

    val jvmInfo = jvmInfo(listOf("bazel-bin/app/lib.jdeps"))
    val t = target("//app:lib", jvmInfo)

    val targetsToImport = mapOf(Label.parse(t.id) to t)
    val result = plugin.collectJdepsLibraries(
      targetsToImport,
      emptyMap(),
      emptyMap(),
      emptyMap(),
    )

    // Empty jdeps file should result in no libraries
    assertTrue(result.isEmpty())
  }

  @Test
  fun `collectJdepsLibraries handles missing jdeps files`() = runBlocking {
    val ws = tmpFolder.newFolder("workspace").toPath()
    val pathsResolver = resolver(ws)
    val plugin = javaPlugin(pathsResolver)

    // Reference a jdeps file that doesn't exist
    val jvmInfo = jvmInfo(listOf("bazel-bin/app/nonexistent.jdeps"))
    val t = target("//app:lib", jvmInfo)

    val targetsToImport = mapOf(Label.parse(t.id) to t)
    val result = plugin.collectJdepsLibraries(
      targetsToImport,
      emptyMap(),
      emptyMap(),
      emptyMap(),
    )

    // Missing jdeps file should result in no libraries
    assertTrue(result.isEmpty())
  }

  @Test
  fun `collectJdepsLibraries skips header jars when processed jar exists`() = runBlocking {
    val ws = tmpFolder.newFolder("workspace").toPath()
    val pathsResolver = resolver(ws)
    val plugin = javaPlugin(pathsResolver)

    // Create both header_ and processed_ jars
    val jarDir = ws.resolve("bazel-bin/external/maven/jar")
    Files.createDirectories(jarDir)
    val headerJar = jarDir.resolve("header_library.jar")
    val processedJar = jarDir.resolve("processed_library.jar")
    Files.write(headerJar, ByteArray(10))
    Files.write(processedJar, ByteArray(10))

    // Create jdeps proto that references the header jar
    val jdepsFile = ws.resolve("bazel-bin/app/lib.jdeps")
    Files.createDirectories(jdepsFile.parent)
    val depsProto = com.google.devtools.build.lib.view.proto.Deps.Dependencies.newBuilder()
      .addDependency(
        com.google.devtools.build.lib.view.proto.Deps.Dependency.newBuilder()
          .setPath(headerJar.toString())
          .setKind(com.google.devtools.build.lib.view.proto.Deps.Dependency.Kind.EXPLICIT)
      )
      .build()
    Files.write(jdepsFile, depsProto.toByteArray())

    val jvmInfo = jvmInfo(listOf("bazel-bin/app/lib.jdeps"))
    val t = target("//app:lib", jvmInfo)

    val targetsToImport = mapOf(Label.parse(t.id) to t)
    val result = plugin.collectJdepsLibraries(
      targetsToImport,
      emptyMap(),
      emptyMap(),
      emptyMap(),
    )

    // Header jar should be skipped because processed jar exists
    assertTrue(result.isEmpty() || result[Label.parse(t.id)].orEmpty().none {
      lib -> lib.outputs.any { it.fileName.toString().contains("header_") }
    })
  }

  @Test
  fun `collectJdepsLibraries deduplicates jars already in interfacesAndBinaries`() = runBlocking {
    val ws = tmpFolder.newFolder("workspace").toPath()
    val pathsResolver = resolver(ws)
    val plugin = javaPlugin(pathsResolver)

    val jarPath = ws.resolve("bazel-bin/app/lib.jar")
    Files.createDirectories(jarPath.parent)
    Files.write(jarPath, ByteArray(10))

    // Create jdeps proto that references this jar
    val jdepsFile = ws.resolve("bazel-bin/app/lib.jdeps")
    Files.createDirectories(jdepsFile.parent)
    val depsProto = com.google.devtools.build.lib.view.proto.Deps.Dependencies.newBuilder()
      .addDependency(
        com.google.devtools.build.lib.view.proto.Deps.Dependency.newBuilder()
          .setPath(jarPath.toString())
          .setKind(com.google.devtools.build.lib.view.proto.Deps.Dependency.Kind.EXPLICIT)
      )
      .build()
    Files.write(jdepsFile, depsProto.toByteArray())

    val jvmInfo = jvmInfo(listOf("bazel-bin/app/lib.jdeps"))
    val t = target("//app:lib", jvmInfo)

    val targetsToImport = mapOf(Label.parse(t.id) to t)
    // Mark the jar as already provided via interfacesAndBinaries
    val interfacesAndBinaries = mapOf(Label.parse(t.id) to setOf(jarPath))

    val result = plugin.collectJdepsLibraries(
      targetsToImport,
      emptyMap(),
      emptyMap(),
      interfacesAndBinaries,
    )

    // Jar should be filtered out because it's in interfacesAndBinaries
    assertTrue(result.isEmpty() || result[Label.parse(t.id)].orEmpty().isEmpty())
  }

  @Test
  fun `collectJdepsLibraries only includes EXPLICIT and IMPLICIT dependencies`() = runBlocking {
    val ws = tmpFolder.newFolder("workspace").toPath()
    val pathsResolver = resolver(ws)
    val plugin = javaPlugin(pathsResolver)

    val explicitJar = ws.resolve("bazel-bin/external/explicit.jar")
    val implicitJar = ws.resolve("bazel-bin/external/implicit.jar")
    val unusedJar = ws.resolve("bazel-bin/external/unused.jar")
    Files.createDirectories(explicitJar.parent)
    Files.write(explicitJar, ByteArray(10))
    Files.write(implicitJar, ByteArray(10))
    Files.write(unusedJar, ByteArray(10))

    // Create jdeps proto with different dependency kinds
    val jdepsFile = ws.resolve("bazel-bin/app/lib.jdeps")
    Files.createDirectories(jdepsFile.parent)
    val depsProto = com.google.devtools.build.lib.view.proto.Deps.Dependencies.newBuilder()
      .addDependency(
        com.google.devtools.build.lib.view.proto.Deps.Dependency.newBuilder()
          .setPath(explicitJar.toString())
          .setKind(com.google.devtools.build.lib.view.proto.Deps.Dependency.Kind.EXPLICIT)
      )
      .addDependency(
        com.google.devtools.build.lib.view.proto.Deps.Dependency.newBuilder()
          .setPath(implicitJar.toString())
          .setKind(com.google.devtools.build.lib.view.proto.Deps.Dependency.Kind.IMPLICIT)
      )
      .addDependency(
        com.google.devtools.build.lib.view.proto.Deps.Dependency.newBuilder()
          .setPath(unusedJar.toString())
          .setKind(com.google.devtools.build.lib.view.proto.Deps.Dependency.Kind.UNUSED)
      )
      .build()
    Files.write(jdepsFile, depsProto.toByteArray())

    val jvmInfo = jvmInfo(listOf("bazel-bin/app/lib.jdeps"))
    val t = target("//app:lib", jvmInfo)

    val targetsToImport = mapOf(Label.parse(t.id) to t)
    val result = plugin.collectJdepsLibraries(
      targetsToImport,
      emptyMap(),
      emptyMap(),
      emptyMap(),
    )

    val libs = result[Label.parse(t.id)].orEmpty()
    val outputPaths = libs.flatMap { it.outputs }.map { it.fileName.toString() }.toSet()

    // Should include explicit and implicit, but not unused
    assertTrue(outputPaths.any { it.contains("explicit") })
    assertTrue(outputPaths.any { it.contains("implicit") })
    assertTrue(outputPaths.none { it.contains("unused") })
  }
}
