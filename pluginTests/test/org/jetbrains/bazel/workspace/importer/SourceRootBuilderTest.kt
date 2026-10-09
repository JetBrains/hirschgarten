package org.jetbrains.bazel.workspace.importer

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.workspace.ProjectViewGlobSet
import org.jetbrains.bazel.workspace.model.test.framework.createTestBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.generatedTestLocation
import org.jetbrains.bazel.workspace.model.test.framework.resolveTestLocation
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
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
      resolveLocation = ::resolveTestLocation,
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
      resolveLocation = ::resolveTestLocation,
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
      resolveLocation = ::resolveTestLocation,
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
    val glob = ProjectViewGlobSet.of(projectRoot, "javatests/*")

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = glob,
      resolveLocation = ::resolveTestLocation,
    )

    roots.first { it.sourcePath == matchingPath }.rootType shouldBe JAVA_TEST_SOURCE_ROOT_TYPE
    roots.first { it.sourcePath == nonMatchingPath }.rootType shouldBe JAVA_SOURCE_ROOT_TYPE
  }

  @Test
  fun `should not mark sources in a sibling directory with the same prefix as test roots`() {
    val projectRoot = Path("/project").toAbsolutePath()
    val matchingPath = projectRoot.resolve("javatests/package/File.java")
    val siblingPath = projectRoot.resolve("javatests_util/package/File.java")
    val target = libraryTarget(
      label = "//target",
      sources = listOf(matchingPath, siblingPath),
    )
    val glob = ProjectViewGlobSet.of(projectRoot, "javatests/*")

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = glob,
      resolveLocation = ::resolveTestLocation,
    )

    roots.first { it.sourcePath == matchingPath }.rootType shouldBe JAVA_TEST_SOURCE_ROOT_TYPE
    roots.first { it.sourcePath == siblingPath }.rootType shouldBe JAVA_SOURCE_ROOT_TYPE
  }

  @Test
  fun `should preserve the generated flag on the source item`() {
    val generatedLocation = generatedTestLocation("gen/Generated.java")
    val generatedPath = resolveTestLocation(generatedLocation)
    val handPath = Path("/project/main/Hand.java")
    val target = libraryTarget(
      label = "//target",
      sources = listOf(handPath),
      generatedSources = listOf(generatedLocation),
    )

    val roots = SourceRootBuilder.resolve(
      target = target,
      testSourcesGlob = ProjectViewGlobSet.EMPTY,
      resolveLocation = ::resolveTestLocation,
    )

    roots.first { it.sourcePath == generatedPath }.generated shouldBe true
    roots.first { it.sourcePath == handPath }.generated shouldBe false
  }

  private fun libraryTarget(
    label: String,
    ruleType: RuleType = RuleType.LIBRARY,
    sources: List<Path> = emptyList(),
    generatedSources: List<OutputLocation> = emptyList(),
    isTestOnly: Boolean = false,
  ): TestBuildTarget = createTestBuildTarget(
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
}
