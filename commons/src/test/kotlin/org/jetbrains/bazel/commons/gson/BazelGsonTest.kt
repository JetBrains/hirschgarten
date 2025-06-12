package org.jetbrains.bazel.commons.gson

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.TargetPattern
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.stream.Stream

class BazelGsonTest {
  @Test
  fun `test BuildTarget serialization`() {
    val buildTarget =
      RawBuildTarget(
        id = Label.parse("//foo:bar"),
        tags = listOf("tag1", "tag2"),
        dependencies = listOf(Label.parse("//baz:qux")),
        kind =
          TargetKind(
            kindString = "java_library",
            languageClasses = setOf(LanguageClass.JAVA, LanguageClass.PYTHON),
            ruleType = RuleType.BINARY,
          ),
        sources = emptyList(),
        resources = emptyList(),
        baseDirectory = Path.of("/base/dir"),
        data =
          PythonBuildTarget(
            version = "3.8",
            interpreter = Path.of("/usr/bin/python3"),
            listOf(),
          ),
      )

    val json = bazelGson.toJson(buildTarget)
    val deserializedBuildTarget = bazelGson.fromJson(json, RawBuildTarget::class.java)
    assertEquals(buildTarget, deserializedBuildTarget)
  }

  @ParameterizedTest
  @MethodSource("labelProvider")
  fun `test Label serialization`(label: Label) {
    val json = bazelGson.toJson(label)
    val deserializedLabel = bazelGson.fromJson(json, Label::class.java)
    assertEquals(label, deserializedLabel)
  }

  @ParameterizedTest
  @MethodSource("targetPatternProvider")
  fun `test TargetPattern serialization`(targetPattern: TargetPattern) {
    val json = bazelGson.toJson(targetPattern)
    val deserializedTargetPattern = bazelGson.fromJson(json, TargetPattern::class.java)
    assertEquals(targetPattern, deserializedTargetPattern)
  }

  @ParameterizedTest
  @MethodSource("pathProvider")
  fun `test Path serialization`(path: Path) {
    val json = bazelGson.toJson(path)
    val deserializedPath = bazelGson.fromJson(json, Path::class.java)
    assertEquals(path, deserializedPath)
  }

  companion object {
    @JvmStatic
    fun labelProvider(): Stream<Arguments> =
      Stream.of(
        Arguments.of(Label.parse("//foo:bar")),
        Arguments.of(Label.parse("@//baz")),
      )

    @JvmStatic
    fun targetPatternProvider(): Stream<Arguments> =
      Stream.of(
        Arguments.of(TargetPattern.parse("//foo:bar")),
        Arguments.of(TargetPattern.parse("//baz/...")),
        Arguments.of(TargetPattern.parse("@//external:all")),
      )

    @JvmStatic
    fun pathProvider(): Stream<Arguments> =
      Stream.of(
        Arguments.of(Path.of("/foo/bar")),
        Arguments.of(Path.of("C:\\baz\\qux")),
        Arguments.of(Path.of("relative/path/to/file.txt")),
      )
  }
}
