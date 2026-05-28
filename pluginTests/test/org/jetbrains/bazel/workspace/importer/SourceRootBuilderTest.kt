package org.jetbrains.bazel.workspace.importer

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixes
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.junit.jupiter.api.DisplayName
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
      sources = listOf(SourceItem(path = sourcePath, generated = false)),
    )

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      packagePrefixes = fixedPrefixes(emptyMap()),
      testTargets = emptySet(),
    )

    roots.map { it.rootType } shouldContainExactly listOf(JAVA_TEST_SOURCE_ROOT_TYPE)
  }

  @Test
  fun `should mark sources of a non-test target as JAVA_SOURCE_ROOT_TYPE`() {
    val sourcePath = Path("/project/main/Foo.java")
    val target = libraryTarget(
      label = "//target",
      sources = listOf(SourceItem(path = sourcePath, generated = false)),
    )

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      packagePrefixes = fixedPrefixes(emptyMap()),
      testTargets = emptySet(),
    )

    roots.map { it.rootType } shouldContainExactly listOf(JAVA_SOURCE_ROOT_TYPE)
  }

  @Test
  fun `should mark sources as test roots when the target id is in testTargets`() {
    val sourcePath = Path("/project/main/Foo.java")
    val targetId = Label.parse("//target")
    val target = libraryTarget(
      label = "//target",
      sources = listOf(SourceItem(path = sourcePath, generated = false)),
    )

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      packagePrefixes = fixedPrefixes(emptyMap()),
      testTargets = setOf(targetId),
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
      sources = listOf(
        SourceItem(path = matchingPath, generated = false),
        SourceItem(path = nonMatchingPath, generated = false),
      ),
    )
    val glob = ProjectViewGlobSet(projectRoot, listOf("javatests/*"))

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = glob,
      packagePrefixes = fixedPrefixes(emptyMap()),
      testTargets = emptySet(),
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
      sources = listOf(
        SourceItem(path = generatedPath, generated = true),
        SourceItem(path = handPath, generated = false),
      ),
    )

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      packagePrefixes = fixedPrefixes(emptyMap()),
      testTargets = emptySet(),
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
      sources = listOf(
        SourceItem(path = withPrefix, generated = false),
        SourceItem(path = withoutPrefix, generated = false),
      ),
    )
    val prefixes = fixedPrefixes(mapOf(withPrefix to "com.example"))

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      packagePrefixes = prefixes,
      testTargets = emptySet(),
    )

    roots.first { it.sourcePath == withPrefix }.packagePrefix shouldBe "com.example"
    roots.first { it.sourcePath == withoutPrefix }.packagePrefix shouldBe ""
  }

  private fun libraryTarget(
    label: String,
    ruleType: RuleType = RuleType.LIBRARY,
    sources: List<SourceItem> = emptyList(),
  ): RawBuildTarget = createRawBuildTarget(
    id = Label.parse(label),
    kind = TargetKind(
      kind = "java_library",
      ruleType = ruleType,
      languageClasses = setOf(LanguageClass.JAVA),
    ),
    sources = sources,
  )

  private fun fixedPrefixes(map: Map<Path, String>): JvmPackagePrefixCalculator =
    object : JvmPackagePrefixCalculator {
      override fun get(target: RawBuildTarget): JvmPackagePrefixes = JvmPackagePrefixes(map)
    }
}
