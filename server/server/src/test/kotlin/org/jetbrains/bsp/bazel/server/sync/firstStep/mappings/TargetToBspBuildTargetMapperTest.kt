package org.jetbrains.bsp.bazel.server.sync.firstStep.mappings

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@Suppress("unused")
private fun data() =
  listOf(
    // given & expected
    arg(
      kind = "java_library",
      expectedLanguage = "java",
      expectedTag = "library",
      expectedCanRun = false,
      expectedCanTest = false,
    ),
    arg(
      kind = "java_binary",
      expectedLanguage = "java",
      expectedTag = "application",
      expectedCanRun = true,
      expectedCanTest = false,
    ),
    arg(
      kind = "java_test",
      expectedLanguage = "java",
      expectedTag = "test",
      expectedCanRun = false,
      expectedCanTest = true,
    ),
    arg(
      kind = "kt_jvm_library",
      expectedLanguage = "kotlin",
      expectedTag = "library",
      expectedCanRun = false,
      expectedCanTest = false,
    ),
    arg(
      kind = "kt_jvm_binary",
      expectedLanguage = "kotlin",
      expectedTag = "application",
      expectedCanRun = true,
      expectedCanTest = false,
    ),
    arg(
      kind = "kt_jvm_test",
      expectedLanguage = "kotlin",
      expectedTag = "test",
      expectedCanRun = false,
      expectedCanTest = true,
    ),
  )

private fun arg(
  kind: String,
  expectedLanguage: String,
  expectedTag: String,
  expectedCanRun: Boolean,
  expectedCanTest: Boolean,
): Arguments = Arguments.of(kind, expectedLanguage, expectedTag, expectedCanRun, expectedCanTest)

@DisplayName("target.toBspBuildTarget() test")
class TargetToBspBuildTargetMapperTest {
  @ParameterizedTest(name = "should map {0} target to build target library with {1} language and canRun: {2} and canTest: {3}")
  @MethodSource("org.jetbrains.bsp.bazel.server.sync.firstStep.mappings.TargetToBspBuildTargetMapperTestKt#data")
  fun `should map target to build target library`(
    kind: String,
    expectedLanguage: String,
    expectedTag: String,
    expectedCanRun: Boolean,
    expectedCanTest: Boolean,
  ) {
    // given
    val target =
      createMockTarget(
        name = "//target_name",
        kind = kind,
        deps = listOf("//target1", "//target2", "//target3"),
      )

    // when
    val bspBuildTarget = target.toBspBuildTarget()

    // then
    bspBuildTarget shouldBe
      BuildTarget(
        BuildTargetIdentifier("//target_name"),
        listOf(expectedTag),
        listOf(expectedLanguage),
        listOf(
          BuildTargetIdentifier("//target1"),
          BuildTargetIdentifier("//target2"),
          BuildTargetIdentifier("//target3"),
        ),
        BuildTargetCapabilities().apply {
          canCompile = true
          canRun = expectedCanRun
          canTest = expectedCanTest
        },
      )
  }

  @Test
  fun `should map manual target to build target with manual tag`() {
    // given
    val target =
      createMockTarget(
        name = "//target_name",
        kind = "java_library",
        deps = listOf("//target1", "//target2", "//target3"),
        tags = listOf("manual"),
      )

    // when
    val bspBuildTarget = target.toBspBuildTarget()

    // then
    bspBuildTarget shouldBe
      BuildTarget(
        BuildTargetIdentifier("//target_name"),
        listOf("library", "manual"),
        listOf("java"),
        listOf(
          BuildTargetIdentifier("//target1"),
          BuildTargetIdentifier("//target2"),
          BuildTargetIdentifier("//target3"),
        ),
        BuildTargetCapabilities().apply {
          canCompile = false
          canRun = false
          canTest = false
        },
      )
  }
}
