package org.jetbrains.bazel.workspace.importer

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixes
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

class CompiledSourceCodeInsideJarExcludeBuilderTest {

  @Test
  fun `should emit Java source name plus matching class file when source is a java file`() {
    val sourcePath = Path("/repo/com/example/Foo.java")
    val target = targetWithSources(listOf(sourcePath))
    val prefixes = fixedPrefixes(mapOf(sourcePath to "com.example"))

    val result = CompiledSourceCodeInsideJarExcludeBuilder
      .calculateRelativePathsInsideJarToExclude(listOf(target), prefixes)

    result shouldContainExactlyInAnyOrder setOf(
      "com/example/Foo.java",
      "com/example/Foo.class",
    )
  }

  @Test
  fun `should emit Kotlin source name plus matching class and file-class for a kt file`() {
    val sourcePath = Path("/repo/com/example/main.kt")
    val target = targetWithSources(listOf(sourcePath))
    val prefixes = fixedPrefixes(mapOf(sourcePath to "com.example"))

    val result = CompiledSourceCodeInsideJarExcludeBuilder
      .calculateRelativePathsInsideJarToExclude(listOf(target), prefixes)

    result shouldContainExactlyInAnyOrder setOf(
      "com/example/main.kt",
      "com/example/main.class",
      "com/example/MainKt.class",
    )
  }

  @Test
  fun `should skip generated sources`() {
    val sourcePath = Path("/repo/com/example/Generated.java")
    val target = targetWithSources(sources = emptyList(), genSources =  listOf(sourcePath))
    val prefixes = fixedPrefixes(mapOf(sourcePath to "com.example"))

    val result = CompiledSourceCodeInsideJarExcludeBuilder
      .calculateRelativePathsInsideJarToExclude(listOf(target), prefixes)

    result shouldContainExactlyInAnyOrder emptySet()
  }

  @Test
  fun `should skip sources whose extension is neither java nor kt`() {
    val sourcePath = Path("/repo/com/example/script.scala")
    val target = targetWithSources(listOf(sourcePath))
    val prefixes = fixedPrefixes(mapOf(sourcePath to "com.example"))

    val result = CompiledSourceCodeInsideJarExcludeBuilder
      .calculateRelativePathsInsideJarToExclude(listOf(target), prefixes)

    result shouldContainExactlyInAnyOrder emptySet()
  }

  @Test
  fun `should omit the package directory prefix when no prefix is configured for the source`() {
    val sourcePath = Path("/repo/Foo.java")
    val target = targetWithSources(listOf(sourcePath))
    val prefixes = fixedPrefixes(emptyMap())

    val result = CompiledSourceCodeInsideJarExcludeBuilder
      .calculateRelativePathsInsideJarToExclude(listOf(target), prefixes)

    result shouldContainExactlyInAnyOrder setOf("Foo.java", "Foo.class")
  }

  @Test
  fun `should capitalize only the first character of kt file-class names`() {
    val sourcePath = Path("/repo/com/example/myUtil.kt")
    val target = targetWithSources(listOf(sourcePath))
    val prefixes = fixedPrefixes(mapOf(sourcePath to "com.example"))

    val result = CompiledSourceCodeInsideJarExcludeBuilder
      .calculateRelativePathsInsideJarToExclude(listOf(target), prefixes)

    result shouldContainExactlyInAnyOrder setOf(
      "com/example/myUtil.kt",
      "com/example/myUtil.class",
      "com/example/MyUtilKt.class",
    )
  }

  @Test
  fun `should produce jar URLs only for libraries flagged as containing internal jars`() {
    val internalLib = LibraryItem(
      key = WorkspaceTargetKey(label = Label.parse("//internal")),
      ijars = emptyList(),
      jars = listOf(Path("/internal/a.jar")),
      sourceJars = listOf(Path("/internal/a-sources.jar")),
      mavenCoordinates = null,
      containsInternalJars = true,
    )
    val externalLib = LibraryItem(
      key = WorkspaceTargetKey(label = Label.parse("//external")),
      ijars = emptyList(),
      jars = listOf(Path("/external/b.jar")),
      sourceJars = emptyList(),
      mavenCoordinates = null,
      containsInternalJars = false,
    )

    val result = CompiledSourceCodeInsideJarExcludeBuilder
      .calculateLibrariesFromInternalTargetsUrls(listOf(internalLib, externalLib))

    result shouldContainExactlyInAnyOrder setOf(
      "jar:///internal/a.jar!/",
      "jar:///internal/a-sources.jar!/",
    )
  }

  @Test
  fun `should also include ijars for internal libraries`() {
    val internalLib = LibraryItem(
      key = WorkspaceTargetKey(label = Label.parse("//internal")),
      ijars = listOf(Path("/internal/a-ijar.jar")),
      jars = listOf(Path("/internal/a.jar")),
      sourceJars = emptyList(),
      mavenCoordinates = null,
      containsInternalJars = true,
    )

    val result = CompiledSourceCodeInsideJarExcludeBuilder
      .calculateLibrariesFromInternalTargetsUrls(listOf(internalLib))

    result shouldContainExactlyInAnyOrder setOf(
      "jar:///internal/a.jar!/",
      "jar:///internal/a-ijar.jar!/",
    )
  }

  private fun targetWithSources(sources: List<Path>, genSources: List<Path> = emptyList()): RawBuildTarget = createRawBuildTarget(
    id = Label.parse("//target"),
    kind = TargetKind(
      kind = "java_library",
      ruleType = RuleType.LIBRARY,
      languageClasses = setOf(JavaLanguageClass.JAVA),
    ),
    sources = sources,
    generatedSources = genSources,
  )

  private fun fixedPrefixes(map: Map<Path, String>): JvmPackagePrefixCalculator =
    object : JvmPackagePrefixCalculator {
      override fun get(target: RawBuildTarget): JvmPackagePrefixes = JvmPackagePrefixes(map)
    }
}
