package org.jetbrains.bazel.workspace.importer

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixes
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

class SourceRootBuilderTest {
  @Test
  fun `should mark sources of a TEST rule as JAVA_TEST_SOURCE_ROOT_TYPE`() {
    val sourcePath = Path("/project/main/Foo.java")
    val target = libraryTarget(
      label = "//target",
      ruleType = RuleType.TEST,
      sources = listOf(sourcePath),
    )

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      packagePrefixes = fixedPrefixes(emptyMap()),
    )

    roots.map { it.rootType } shouldContainExactly listOf(JAVA_TEST_SOURCE_ROOT_TYPE)
  }

  @Test
  fun `should mark sources of a non-test target as JAVA_SOURCE_ROOT_TYPE`() {
    val sourcePath = Path("/project/main/Foo.java")
    val target = libraryTarget(
      label = "//target",
      sources = listOf(sourcePath),
    )

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      packagePrefixes = fixedPrefixes(emptyMap()),
    )

    roots.map { it.rootType } shouldContainExactly listOf(JAVA_SOURCE_ROOT_TYPE)
  }

  @Test
  fun `should mark sources of a testonly target as test roots`() {
    val sourcePath = Path("/project/main/Foo.java")
    val target = libraryTarget(
      label = "//target",
      sources = listOf(sourcePath),
      isTestOnly = true,
    )

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      packagePrefixes = fixedPrefixes(emptyMap()),
    )

    roots.map { it.rootType } shouldContainExactly listOf(JAVA_TEST_SOURCE_ROOT_TYPE)
  }

  @Test
  fun `should mark sources matching testSourcesGlob as test roots`() {
    val projectRoot = Path("/project").toAbsolutePath()
    val matchingPath = projectRoot.resolve("javatests/package/File.java")
    val nonMatchingPath = projectRoot.resolve("main/package/File.java")
    val target = libraryTarget(
      label = "//target",
      sources = listOf(matchingPath, nonMatchingPath),
    )
    val glob = ProjectViewGlobSet(projectRoot, listOf("javatests/*"))

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = glob,
      packagePrefixes = fixedPrefixes(emptyMap()),
    )

    roots.first { it.sourcePath == matchingPath }.rootType shouldBe JAVA_TEST_SOURCE_ROOT_TYPE
    roots.first { it.sourcePath == nonMatchingPath }.rootType shouldBe JAVA_SOURCE_ROOT_TYPE
  }

  @Test
  fun `should preserve the generated flag on the source item`() {
    val generatedPath = Path("/project/gen/Generated.java")
    val handPath = Path("/project/main/Hand.java")
    val target = libraryTarget(
      label = "//target",
      sources = listOf(handPath),
      generatedSources = listOf(generatedPath),
    )

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      packagePrefixes = fixedPrefixes(emptyMap()),
    )

    roots.first { it.sourcePath == generatedPath }.generated shouldBe true
    roots.first { it.sourcePath == handPath }.generated shouldBe false
  }

  @Test
  fun `should inject packagePrefix from the calculator and default to empty string when missing`() {
    val withPrefix = Path("/project/main/Foo.java")
    val withoutPrefix = Path("/project/main/Bar.java")
    val target = libraryTarget(
      label = "//target",
      sources = listOf(withPrefix, withoutPrefix),
    )
    val prefixes = fixedPrefixes(mapOf(withPrefix to "com.example"))

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      packagePrefixes = prefixes,
    )

    roots.first { it.sourcePath == withPrefix }.packagePrefix shouldBe "com.example"
    roots.first { it.sourcePath == withoutPrefix }.packagePrefix shouldBe ""
  }

  private fun libraryTarget(
    label: String,
    ruleType: RuleType = RuleType.LIBRARY,
    sources: List<Path> = emptyList(),
    generatedSources: List<Path> = emptyList(),
    isTestOnly: Boolean = false,
  ): RawBuildTarget = createRawBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "java_library",
      ruleType = ruleType,
      languageClasses = setOf(JavaLanguageClass.JAVA),
    ),
    sources = sources,
    generatedSources = generatedSources,
    isTestOnly = isTestOnly,
  )

  private fun fixedPrefixes(map: Map<Path, String>): JvmPackagePrefixCalculator =
    object : JvmPackagePrefixCalculator {
      override fun get(target: RawBuildTarget): JvmPackagePrefixes = JvmPackagePrefixes(map)
    }
}
