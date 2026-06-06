package org.jetbrains.bazel.commons.gson

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.target.targetUtilsGson
import org.jetbrains.bsp.protocol.SourceFileCollection
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
        dependencies = listOf(DependencyLabel.parse("//baz:qux")),
        kind =
          TargetKind(
            kind = "java_library",
            languageClasses = setOf(LanguageClass.JAVA, LanguageClass.PYTHON),
            ruleType = RuleType.BINARY,
          ),
        sources = SourceFileCollection.EMPTY,
        generatedSources = SourceFileCollection.EMPTY,
        resources = SourceFileCollection.EMPTY,
        baseDirectory = Path.of("/base/dir"),
        data = listOf(
          PythonBuildTarget(
            version = "3.8",
            interpreter = Path.of("/usr/bin/python3"),
            listOf(),
            null,
            null,
            Path.of("/base/dir/main.py"),
            null,
          ),
        ),
        isManual = false,
      )

    val json = targetUtilsGson.toJson(buildTarget)
    val deserializedBuildTarget = targetUtilsGson.fromJson(json, RawBuildTarget::class.java)
    assertEquals(buildTarget, deserializedBuildTarget)
  }

  @ParameterizedTest
  @MethodSource("labelProvider")
  fun `test Label serialization`(label: Label) {
    val json = targetUtilsGson.toJson(label)
    val deserializedLabel = targetUtilsGson.fromJson(json, Label::class.java)
    assertEquals(label, deserializedLabel)
  }

  @ParameterizedTest
  @MethodSource("pathProvider")
  fun `test Path serialization`(path: Path) {
    val json = targetUtilsGson.toJson(path)
    val deserializedPath = targetUtilsGson.fromJson(json, Path::class.java)
    assertEquals(path, deserializedPath)
  }

  @ParameterizedTest
  @MethodSource("sourceFileCollectionProvider")
  fun `test SourceFileCollection round-trip preserves files`(paths: List<Path>, relativeRoot: Path?) {
    val original = SourceFileCollectionBuilder.build(relativeRoot, paths)

    val json = targetUtilsGson.toJson(original, SourceFileCollection::class.java)
    val deserialized = targetUtilsGson.fromJson(json, SourceFileCollection::class.java)

    assertEquals(original.getFiles().toSet(), deserialized.getFiles().toSet())
    assertEquals(original.isEmpty(), deserialized.isEmpty())
  }

  companion object {
    @JvmStatic
    fun labelProvider(): Stream<Arguments> =
      Stream.of(
        Arguments.of(Label.parse("//foo:bar")),
        Arguments.of(Label.parse("@//baz")),
      )

    @JvmStatic
    fun pathProvider(): Stream<Arguments> =
      Stream.of(
        Arguments.of(Path.of("/foo/bar")),
        Arguments.of(Path.of("C:\\baz\\qux")),
        Arguments.of(Path.of("relative/path/to/file.txt")),
      )

    @JvmStatic
    fun sourceFileCollectionProvider(): Stream<Arguments> {
      val root = Path.of("/workspace/foo")
      return Stream.of(
        Arguments.of(
          listOf(
            root.resolve("src/Main.kt"),
            root.resolve("src/Util.kt"),
            root.resolve("resources/x.txt"),
          ),
          root,
        ),
        Arguments.of(
          listOf(
            Path.of("/elsewhere/Other.kt"),
            Path.of("/var/cache/Generated.kt"),
          ),
          root,
        ),
        Arguments.of(
          listOf(
            root.resolve("src/Main.kt"),
            Path.of("/elsewhere/Other.kt"),
          ),
          root,
        ),
        Arguments.of(
          listOf(Path.of("/a/foo.kt"), Path.of("/b/bar.kt")),
          null,
        ),
      )
    }
  }
}
