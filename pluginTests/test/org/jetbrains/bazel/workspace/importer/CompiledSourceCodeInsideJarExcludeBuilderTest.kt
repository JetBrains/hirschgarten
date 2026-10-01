package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.createTestBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.generatedTestLocation
import org.jetbrains.bazel.workspace.model.test.framework.resolveTestLocation
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

class CompiledSourceCodeInsideJarExcludeBuilderTest {

  @Test
  fun `should collect java and kt source file paths`() {
    val javaSource = Path("/repo/src/com/example/Foo.java")
    val kotlinSource = Path("/repo/src/com/example/main.kt")
    val target = targetWithSources(listOf(javaSource, kotlinSource))

    val result = CompiledSourceCodeInsideJarExcludeBuilder.calculateSourceFilePaths(listOf(target), ::resolveTestLocation)

    result shouldContainExactlyInAnyOrder setOf(
      "/repo/src/com/example/Foo.java",
      "/repo/src/com/example/main.kt",
    )
  }

  @Test
  fun `should skip generated sources`() {
    val sourcePath = Path("/repo/com/example/Generated.java")
    val target = targetWithSources(sources = emptyList(), genSources = listOf(sourcePath))

    val result = CompiledSourceCodeInsideJarExcludeBuilder.calculateSourceFilePaths(listOf(target), ::resolveTestLocation)

    result shouldContainExactlyInAnyOrder emptySet()
  }

  @Test
  fun `should skip sources whose extension is neither java nor kt`() {
    val sourcePath = Path("/repo/com/example/script.scala")
    val target = targetWithSources(listOf(sourcePath))

    val result = CompiledSourceCodeInsideJarExcludeBuilder.calculateSourceFilePaths(listOf(target), ::resolveTestLocation)

    result shouldContainExactlyInAnyOrder emptySet()
  }

  @Test
  fun `should match java class and source by package approximated from the path inside jar`() {
    val index = CompiledSourceFileIndex(setOf("/repo/src/main/java/com/example/Foo.java"))

    index.hasSourceFor("com/example/Foo.class") shouldBe true
    index.hasSourceFor("com/example/Foo.java") shouldBe true
    index.hasSourceFor("example/Foo.class") shouldBe true
    index.hasSourceFor("com/other/Foo.class") shouldBe false
    index.hasSourceFor("org/com/example/Foo.class") shouldBe false
    index.hasSourceFor("com/example/Bar.class") shouldBe false
    index.hasSourceFor("com/example/Foo.kt") shouldBe false
  }

  @Test
  fun `should not match a directory which only ends with the same characters as the package`() {
    val index = CompiledSourceFileIndex(setOf("/repo/src/mycom/example/Foo.java"))

    index.hasSourceFor("com/example/Foo.class") shouldBe false
  }

  @Test
  fun `should match classes in the default package with a source of the same name`() {
    val index = CompiledSourceFileIndex(setOf("/repo/Foo.java"))

    index.hasSourceFor("Foo.class") shouldBe true
    index.hasSourceFor("Bar.class") shouldBe false
  }

  @Test
  fun `should match kotlin classes and file-classes`() {
    val index = CompiledSourceFileIndex(setOf("/repo/src/com/example/main.kt", "/repo/src/com/example/Util.kt"))

    index.hasSourceFor("com/example/main.kt") shouldBe true
    index.hasSourceFor("com/example/main.class") shouldBe true
    index.hasSourceFor("com/example/MainKt.class") shouldBe true
    index.hasSourceFor("com/example/Util.class") shouldBe true
    index.hasSourceFor("com/example/UtilKt.class") shouldBe true
    index.hasSourceFor("com/example/Kt.class") shouldBe false
    index.hasSourceFor("com/other/MainKt.class") shouldBe false
  }

  @Test
  fun `should not match non-JVM files`() {
    val index = CompiledSourceFileIndex(setOf("/repo/src/com/example/Foo.java"))

    index.hasSourceFor("com/example/Foo.xml") shouldBe false
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

  @Test
  fun `should reuse the exclude ID when the content is unchanged`() {
    val target = targetWithSources(listOf(Path("/repo/src/com/example/Foo.java")))
    val current = writeExcludeEntity(listOf(target), currentExcludeEntity = null)

    val next = writeExcludeEntity(listOf(target), currentExcludeEntity = current)

    next.excludeId shouldBeSameInstanceAs current.excludeId
  }

  @Test
  fun `should use a new exclude ID when the content changes`() {
    val current = writeExcludeEntity(listOf(targetWithSources(listOf(Path("/repo/src/com/example/Foo.java")))), currentExcludeEntity = null)

    val next = writeExcludeEntity(listOf(targetWithSources(listOf(Path("/repo/src/com/example/Bar.java")))), currentExcludeEntity = current)

    next.excludeId shouldNotBe current.excludeId
  }

  @Test
  fun `should compute the same exclude ID for the same content without the current entity`() {
    val target = targetWithSources(listOf(Path("/repo/src/com/example/Foo.java")))

    val first = writeExcludeEntity(listOf(target), currentExcludeEntity = null)
    val second = writeExcludeEntity(listOf(target), currentExcludeEntity = null)

    second.excludeId shouldBe first.excludeId
  }

  private fun writeExcludeEntity(
    targets: List<BuildTarget>,
    currentExcludeEntity: CompiledSourceCodeInsideJarExcludeEntity?,
  ): CompiledSourceCodeInsideJarExcludeEntity {
    val internalLibrary = LibraryItem(
      key = WorkspaceTargetKey(label = Label.parse("//internal")),
      ijars = emptyList(),
      jars = listOf(Path("/internal/a.jar")),
      sourceJars = emptyList(),
      mavenCoordinates = null,
      containsInternalJars = true,
    )
    val storage = MutableEntityStorage.create()
    CompiledSourceCodeInsideJarExcludeBuilder.write(targets, listOf(internalLibrary), ::resolveTestLocation, storage, currentExcludeEntity)
    return storage.toSnapshot().entities<CompiledSourceCodeInsideJarExcludeEntity>().single()
  }

  private fun targetWithSources(sources: List<Path>, genSources: List<Path> = emptyList()): TestBuildTarget = createTestBuildTarget(
    id = Label.parse("//target"),
    kind = TargetKind(
      kind = "java_library",
      ruleType = RuleType.LIBRARY,
      languageClasses = setOf(JavaLanguageClass.JAVA),
    ),
    sources = sources,
    generatedSources = genSources.map {generatedTestLocation(it.invariantSeparatorsPathString)},
  )
}
