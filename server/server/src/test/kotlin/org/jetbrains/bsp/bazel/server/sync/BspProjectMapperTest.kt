package org.jetbrains.bsp.bazel.server.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencyModule
import ch.epfl.scala.bsp4j.DependencyModulesItem
import ch.epfl.scala.bsp4j.DependencyModulesParams
import ch.epfl.scala.bsp4j.DependencyModulesResult
import ch.epfl.scala.bsp4j.MavenDependencyModule
import ch.epfl.scala.bsp4j.MavenDependencyModuleArtifact
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.server.model.AspectSyncProject
import org.jetbrains.bsp.bazel.server.model.Library
import org.jetbrains.bsp.bazel.server.model.Module
import org.jetbrains.bsp.bazel.server.model.SourceSet
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class BspProjectMapperTest {
  private val cacheLocation =
    "file:///home/user/.cache/bazel/_bazel_user/ae7b7b315151086e31e3b97f9ddba009/execroot/monorepo/bazel-out/k8-fastbuild-ST-4a519fd6d3e4"

  private val mavenCoordinatesResolver = MavenCoordinatesResolver()

  @Test
  @Timeout(value = 3, unit = TimeUnit.MINUTES)
  fun `should compute buildDependencyModules quickly`() {
    // Make sure we can compute dependency modules for a large number of targets, each of which has a large number of dependencies.
    // Large enough to time out if using a non-optimized algorithm.
    val libCount = 20000
    val mavenDepCount = 1000
    val allLibraries = mutableListOf<Library>()
    val expectedDepModules = mutableListOf<DependencyModule>()
    val currentUri = Paths.get(".").toUri()
    var allModules = mutableListOf<Module>()
    for (i in 0..mavenDepCount - 1) {
      val jarUri =
        URI.create(
          "$cacheLocation/bin/external/maven/org/scala-lang/scala-library/2.13." + i + "/processed_scala-library-2.13." + i + ".jar",
        )
      val jarSourcesUri =
        URI.create(
          "$cacheLocation/bin/external/maven/org/scala-lang/scala-library/2.13." + i + "/scala-library-2.13." + i + "-sources.jar",
        )

      val labelStr = "@maven//:org_scala_lang_scala_library" + String.format("%04d", i)
      val label = Label.parse(labelStr)
      allLibraries.add(
        Library(
          label,
          setOf(jarUri),
          setOf(jarSourcesUri),
          emptyList(),
          emptySet(),
          mavenCoordinatesResolver.resolveMavenCoordinates(label, jarUri),
        ),
      )

      allModules.add(
        Module(
          label,
          true,
          emptyList(),
          emptySet(),
          emptySet(),
          currentUri,
          SourceSet(emptySet(), emptySet(), emptySet()),
          emptySet(),
          emptySet(),
          emptySet(),
          null,
          emptyMap(),
        ),
      )

      val mavenSourceArtifact =
        MavenDependencyModuleArtifact(
          jarSourcesUri.toString(),
        )
      mavenSourceArtifact.setClassifier("sources")
      val expectedDepModule = DependencyModule(labelStr, "2.13." + i)
      expectedDepModule.setDataKind("maven")
      expectedDepModule.setData(
        MavenDependencyModule(
          "org.scala-lang",
          "scala-library",
          "2.13." + i,
          listOf<MavenDependencyModuleArtifact>(
            MavenDependencyModuleArtifact(
              jarUri.toString(),
            ),
            mavenSourceArtifact,
          ),
        ),
      )
      expectedDepModules.add(expectedDepModule)
    }

    // Common deps for each target
    val commonDeps = allLibraries.subList(0, mavenDepCount).map { it.label }

    for (i in 0..libCount - 1) {
      val label = Label.parse("//foo/bar:target" + i)
      allLibraries.add(
        Library(
          label,
          emptySet(),
          emptySet(),
          commonDeps,
        ),
      )

      allModules.add(
        Module(
          label,
          true,
          commonDeps,
          emptySet(),
          emptySet(),
          currentUri,
          SourceSet(emptySet(), emptySet(), emptySet()),
          emptySet(),
          emptySet(),
          emptySet(),
          null,
          emptyMap(),
        ),
      )
    }

    val libraries = allLibraries.associate({ it.label to it })
    val project =
      AspectSyncProject(
        workspaceRoot = currentUri,
        bazelRelease = BazelRelease(6),
        modules = allModules,
        libraries = libraries,
        goLibraries = emptyMap(),
        invalidTargets = emptyList(),
        nonModuleTargets = emptyList(),
      )

    val deps =
      BspProjectMapper.buildDependencyModulesStatic(
        project,
        DependencyModulesParams(
          allLibraries.subList(mavenDepCount, mavenDepCount + libCount).map { BuildTargetIdentifier(it.label.toString()) },
        ),
      )

    deps shouldBe
      DependencyModulesResult(
        allLibraries.subList(mavenDepCount, mavenDepCount + libCount).map {
          DependencyModulesItem(
            BuildTargetIdentifier(it.label.toString()),
            expectedDepModules,
          )
        },
      )
  }
}
