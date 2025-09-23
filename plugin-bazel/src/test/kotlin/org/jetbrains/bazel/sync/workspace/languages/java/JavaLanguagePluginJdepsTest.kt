package org.jetbrains.bazel.sync.workspace.languages.java

import com.google.devtools.build.lib.view.proto.Deps
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.JvmOutputs
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class JavaLanguagePluginJdepsTest {
  private fun resolver(root: Path): BazelPathsResolver = BazelPathsResolverMock.create(root)

  private fun fileLocation(path: String): FileLocation = FileLocation.newBuilder().setRelativePath(path).build()

  private fun jvmOutputs(bin: String? = null, iface: String? = null, src: String? = null): JvmOutputs =
    JvmOutputs.newBuilder()
      .apply { if (bin != null) addBinaryJars(fileLocation(bin)) }
      .apply { if (iface != null) addInterfaceJars(fileLocation(iface)) }
      .apply { if (src != null) addSourceJars(fileLocation(src)) }
      .build()

  private fun jvmTargetInfo(
    outputs: List<JvmOutputs> = emptyList(),
    generated: List<JvmOutputs> = emptyList(),
    jdepsPaths: List<String> = emptyList(),
  ): JvmTargetInfo =
    JvmTargetInfo.newBuilder()
      .addAllJars(outputs)
      .addAllGeneratedJars(generated)
      .apply { jdepsPaths.forEach { addJdeps(fileLocation(it)) } }
      .build()

  private fun target(id: String, kind: String, jvm: JvmTargetInfo, deps: List<String> = emptyList()): TargetInfo =
    TargetInfo.newBuilder()
      .setId(id)
      .setKind(kind)
      .setJvmTargetInfo(jvm)
      .addAllDependencies(deps.map { BspTargetInfo.Dependency.newBuilder().setId(it).build() })
      .build()

  @Test
  fun `collectJdepsLibraries filters excluded and header_ jars and dedupes against outputs`() = runBlocking {
    val ws = Files.createTempDirectory("ws")
    val pathsResolver = resolver(ws)
    val plugin = JavaLanguagePlugin(pathsResolver, JdkResolver(pathsResolver, JdkVersionResolver()), DefaultJvmPackageResolver(), org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver())

    // Prepare files on disk matching .jdeps content
    val outputJar = ws.resolve("bazel-bin/app/libapp.jar").apply { Files.createDirectories(parent); Files.createFile(this) }
    val headerJar = ws.resolve("external/m2/header_guava.jar").apply { Files.createDirectories(parent); Files.createFile(this) }
    val processedJar = ws.resolve("external/m2/processed_guava.jar").apply { Files.createFile(this) }
    val extraJar = ws.resolve("external/m2/commons-lang3-3.12.0.jar").apply { Files.createFile(this) }

    // .jdeps referencing: output (should be excluded), header_ + processed_ (header should be skipped), and an extra jar (should be included)
    val depsProto = Deps.Dependencies.newBuilder()
      .addDependency(Deps.Dependency.newBuilder().setKind(Deps.Dependency.Kind.EXPLICIT).setPath(outputJar.toString()))
      .addDependency(Deps.Dependency.newBuilder().setKind(Deps.Dependency.Kind.IMPLICIT).setPath(headerJar.toString()))
      .addDependency(Deps.Dependency.newBuilder().setKind(Deps.Dependency.Kind.EXPLICIT).setPath(processedJar.toString()))
      .addDependency(Deps.Dependency.newBuilder().setKind(Deps.Dependency.Kind.EXPLICIT).setPath(extraJar.toString()))
      .build()

    ws.resolve("bazel-bin/app").toFile().mkdirs()
    val jdepsFile = ws.resolve("bazel-bin/app/libapp.jdeps").apply {
      Files.newOutputStream(this).use { depsProto.writeTo(it) }
    }

    // Target produces libapp.jar itself
    val tJvm = jvmTargetInfo(
      outputs = listOf(jvmOutputs(bin = "bazel-bin/app/libapp.jar")),
      generated = emptyList(),
      jdepsPaths = listOf("bazel-bin/app/libapp.jdeps"),
    )
    val t = target("//app:lib", "java_library", tJvm)

    val perTarget = emptyMap<Label, List<org.jetbrains.bazel.sync.workspace.model.Library>>()
    val allKnown = emptyMap<Label, org.jetbrains.bazel.sync.workspace.model.Library>()
    val targetMap = mapOf(Label.parse(t.id) to t)
    val ib = mapOf(Label.parse(t.id) to setOf(outputJar))

    val jdepsLibs = plugin.collectJdepsLibraries(targetMap, perTarget, allKnown, ib)

    // Expect: extraJar included, headerJar skipped (due to processed_), outputJar excluded
    val libs = jdepsLibs[Label.parse(t.id)].orEmpty()
    val jarNames = libs.flatMap { it.outputs }.map { it.fileName.toString() }.toSet()
    // Ensure header_ is filtered when processed_ exists and target output is excluded
    assertTrue("header_ should be filtered when processed_ exists", jarNames.none { it.startsWith("header_") })
    assertTrue("target output should be excluded", jarNames.none { it == "libapp.jar" })
  }

  @Test
  fun `collectJdepsLibraries removes jars provided transitively by dependencies`() = runBlocking {
    val ws = Files.createTempDirectory("ws2")
    val pathsResolver = resolver(ws)
    val plugin = JavaLanguagePlugin(pathsResolver, JdkResolver(pathsResolver, JdkVersionResolver()), DefaultJvmPackageResolver(), org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver())

    val transitiveJar = ws.resolve("external/m2/transitive.jar").apply { Files.createDirectories(parent); Files.createFile(this) }

    // Dep target produces transitive.jar as its own output
    val depJvm = jvmTargetInfo(outputs = listOf(jvmOutputs(bin = "external/m2/transitive.jar")))
    val dep = target("//lib:dep", "java_library", depJvm)

    // Root target jdeps references the same jar, which should be dropped as transitive
    val jdepsProto = Deps.Dependencies.newBuilder()
      .addDependency(Deps.Dependency.newBuilder().setKind(Deps.Dependency.Kind.EXPLICIT).setPath(transitiveJar.toString()))
      .build()
    ws.resolve("bazel-bin/app").toFile().mkdirs()
    val jdepsFile = ws.resolve("bazel-bin/app/app.jdeps").apply {
      Files.newOutputStream(this).use { jdepsProto.writeTo(it) }
    }

    val rootJvm = jvmTargetInfo(jdepsPaths = listOf("bazel-bin/app/app.jdeps"))
    val root = target("//app:root", "java_library", rootJvm, deps = listOf(dep.id))

    val targetsToImport = mapOf(Label.parse(root.id) to root, Label.parse(dep.id) to dep)

    val perTargetLibs = mapOf<Label, List<org.jetbrains.bazel.sync.workspace.model.Library>>()
    val allKnownLibs = mapOf(
      Label.parse(dep.id) to org.jetbrains.bazel.sync.workspace.model.Library(
        label = Label.parse(dep.id),
        outputs = setOf(transitiveJar),
        sources = emptySet(),
        dependencies = emptyList(),
      )
    )
    val ib = mapOf(Label.parse(root.id) to emptySet(), Label.parse(dep.id) to setOf(transitiveJar))

    val jdepsLibs = plugin.collectJdepsLibraries(targetsToImport, perTargetLibs, allKnownLibs, ib)
    val libsForRoot = jdepsLibs[Label.parse(root.id)].orEmpty()
    val jarNames = libsForRoot.flatMap { it.outputs }.map { it.fileName.toString() }.toSet()

    // The jar is provided by dependency dep -> should be removed from root's jdeps-derived set
    assertTrue(jarNames.isEmpty())
  }
}
