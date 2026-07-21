package org.jetbrains.bazel.sync.workspace.mapper.normal

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.MavenCoordinates
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

// Test data based on https://github.com/JetBrains/bazel-bsp/pull/536
class MavenCoordinatesResolverTest {
  private val cacheLocation =
    Path("/home/user/.cache/bazel/_bazel_user/ae7b7b315151086e31e3b97f9ddba009/execroot/monorepo/bazel-out/k8-fastbuild-ST-4a519fd6d3e4")

  @Test
  fun `should resolve maven coordinates correctly with Bazel 7`() {
    val label = Label.parse("@maven//:org_scala_lang_scala_library")
    val outputJar = cacheLocation.resolve("bin/external/maven/org/scala-lang/scala-library/2.13.11/processed_scala-library-2.13.11.jar")
    val expectedMavenCoordinates = MavenCoordinates(
      "org.scala-lang",
      "scala-library",
      "2.13.11",
    )
    MavenCoordinatesResolver.resolveMavenCoordinates(label, outputJar) shouldBe expectedMavenCoordinates
  }

  @Test
  fun `should resolve maven coordinates correctly with bzlmod`() {
    val label = Label.parse("@@rules_jvm_external~override~maven~maven//:com_google_auto_service_auto_service_annotations")
    val outputJar =
      cacheLocation.resolve("bin/external/rules_jvm_external~~maven~name/v1/https/repo1.maven.org/maven2/com/google/auto/service/auto-service-annotations/1.1.1/header_auto-service-annotations-1.1.1.jar")
    val expectedMavenCoordinates = MavenCoordinates(
      "com.google.auto.service",
      "auto-service-annotations",
      "1.1.1",
    )
    MavenCoordinatesResolver.resolveMavenCoordinates(label, outputJar) shouldBe expectedMavenCoordinates
  }

  @Test
  fun `should not resolve non-maven dependency`() {
    val label = Label.parse("@//projects/v1:scheduler")
    val outputJar = cacheLocation.resolve("bin/projects/v1/scheduler.jar")
    MavenCoordinatesResolver.resolveMavenCoordinates(label, outputJar) shouldBe null
  }

  @Test
  fun `should not resolve non-jar file`() {
    val label = Label.parse("@@rules_jvm_external~override~maven~maven//:com_google_auto_service_auto_service_annotations")
    val outputJar =
      cacheLocation.resolve("bin/external/rules_jvm_external~~maven~name/v1/https/repo1.maven.org/maven2/com/google/auto/service/auto-service-annotations/1.1.1/header_auto-service-annotations-1.1.1.xml")
    MavenCoordinatesResolver.resolveMavenCoordinates(label, outputJar) shouldBe null
  }

  @Test
  fun `should not get an IndexOutOfBoundsException on a short path`() {
    val label = Label.parse("@@rules_jvm_external~override~maven~maven//:com_google_auto_service_auto_service_annotations")
    val outputJar = Path("rules_jvm_external~~maven~name/auto-service-annotations/header_auto-service-annotations-1.1.1.jar")
    MavenCoordinatesResolver.resolveMavenCoordinates(label, outputJar) shouldBe null
  }

  @Test
  fun `should resolve maven coordinates from a tags list containing the maven_coordinates tag`() {
    val tags = listOf("manual", "no-ide", "maven_coordinates=com.google.guava:guava:31.1-jre")
    val expectedMavenCoordinates = MavenCoordinates(
      "com.google.guava",
      "guava",
      "31.1-jre",
    )
    MavenCoordinatesResolver.fromTargetTagsList(tags) shouldBe expectedMavenCoordinates
  }

  @Test
  fun `should resolve maven coordinates when maven_coordinates is the only tag`() {
    val tags = listOf("maven_coordinates=org.scala-lang:scala-library:2.13.11")
    val expectedMavenCoordinates = MavenCoordinates(
      "org.scala-lang",
      "scala-library",
      "2.13.11",
    )
    MavenCoordinatesResolver.fromTargetTagsList(tags) shouldBe expectedMavenCoordinates
  }

  @Test
  fun `should return null when the tags list is empty`() {
    MavenCoordinatesResolver.fromTargetTagsList(emptyList()) shouldBe null
  }

  @Test
  fun `should return null when no maven_coordinates tag is present`() {
    val tags = listOf("manual", "no-ide")
    MavenCoordinatesResolver.fromTargetTagsList(tags) shouldBe null
  }

  @Test
  fun `should return null when maven_coordinates value has only groupId`() {
    val tags = listOf("maven_coordinates=com.google.guava")
    MavenCoordinatesResolver.fromTargetTagsList(tags) shouldBe null
  }

  @Test
  fun `should return null when maven_coordinates value is missing the version`() {
    val tags = listOf("maven_coordinates=com.google.guava:guava")
    MavenCoordinatesResolver.fromTargetTagsList(tags) shouldBe null
  }

  @Test
  fun `should return null when maven_coordinates value is empty`() {
    val tags = listOf("maven_coordinates=")
    MavenCoordinatesResolver.fromTargetTagsList(tags) shouldBe null
  }

  @Test
  fun `should pick the first maven_coordinates tag when multiple are present`() {
    val tags = listOf(
      "maven_coordinates=g1:a1:1.0",
      "maven_coordinates=g2:a2:2.0",
    )
    val expectedMavenCoordinates = MavenCoordinates(
      "g1",
      "a1",
      "1.0",
    )
    MavenCoordinatesResolver.fromTargetTagsList(tags) shouldBe expectedMavenCoordinates
  }

  @Test
  fun `should ignore extra colon-separated segments beyond version`() {
    val tags = listOf("maven_coordinates=com.example:artifact:1.2.3:sources")
    val expectedMavenCoordinates = MavenCoordinates(
      "com.example",
      "artifact",
      "1.2.3",
    )
    MavenCoordinatesResolver.fromTargetTagsList(tags) shouldBe expectedMavenCoordinates
  }
}
