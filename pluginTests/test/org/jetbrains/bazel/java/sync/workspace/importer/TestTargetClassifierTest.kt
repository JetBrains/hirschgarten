package org.jetbrains.bazel.java.sync.workspace.importer

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.importer.TestTargetClassifier
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.Test
import java.nio.file.Path

class TestTargetClassifierTest {
  @Test
  fun `should mark test target`() {
    val target = createTarget("//aaa/bbb:ccc", javaTest, sources = listOf("Test.java"))

    classify(listOf(target)) shouldContain target
  }

  @Test
  fun `should mark test-only target`() {
    val target = createTarget("//aaa/bbb:ccc", javaLibrary, sources = listOf("Library.java"), isTestOnly = true)

    classify(listOf(target)) shouldContain target
  }

  @Test
  fun `should not mark binary target`() {
    val target = createTarget("//aaa/bbb:ccc", javaBinary, sources = listOf("Binary.java"))

    classify(listOf(target)) shouldNotContain target
  }

  @Test
  fun `should not mark library not related to any tests`() {
    val library1 = createTarget("//aaa/bbb:library1", javaLibrary)
    val library2 = createTarget("//aaa/bbb:library2", javaLibrary, sources = listOf("Library.java"))
    val library3 = createTarget("//aaa/bbb:library3", javaLibrary, resources = listOf("Resource.txt"))

    val result = classify(listOf(library1, library2, library3))

    result shouldNotContain library1
    result shouldNotContain library2
    result shouldNotContain library3
  }

  @Test
  fun `should not mark library used in a non-test target`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val binary = createTarget("//aaa/bbb:binary", javaBinary, dependencies = listOf(library), sources = listOf("Binary.java"))
    val test = createTarget("//aaa/bbb:test", javaTest, dependencies = listOf(library))

    classify(listOf(binary, test, library)) shouldNotContain library
  }

  @Test
  fun `should not mark library used in test target that has its own sources`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val test = createTarget("//aaa/bbb:test", javaTest, dependencies = listOf(library), sources = listOf("Test.java"))

    classify(listOf(test, library)) shouldNotContain library
  }

  @Test
  fun `should mark library used in test target without own sources`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val test = createTarget("//aaa/bbb:test", javaTest, dependencies = listOf(library))

    classify(listOf(test, library)) shouldContain library
  }

  @Test
  fun `should mark library used in multiple test targets, some without own sources`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val test1 = createTarget("//aaa/bbb:test1", javaTest, dependencies = listOf(library))
    val test2 = createTarget("//aaa/bbb:test2", javaTest, dependencies = listOf(library), sources = listOf("Test.java"))
    val test3 = createTarget("//aaa/bbb:test3", javaTest, dependencies = listOf(library))

    classify(listOf(test1, test2, test3, library)) shouldContain library
  }

  @Test
  fun `should not mark library used in a non-test library`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val test = createTarget("//aaa/bbb:test", javaTest, dependencies = listOf(library))
    val consumer = createTarget("//aaa/ccc:consumer", javaLibrary, dependencies = listOf(library), sources = listOf("Consumer.java"))

    classify(listOf(test, consumer, library)) shouldNotContain library
  }

  @Test
  fun `should not mark library used in a test-only library`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val testLibrary = createTarget("//aaa/bbb:library_test_lib", javaLibrary, dependencies = listOf(library), isTestOnly = true)

    classify(listOf(testLibrary, library)) shouldNotContain library
  }

  @Test
  fun `should not mark library used in test target transitively`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val intermediateLibrary = createTarget("//aaa/bbb:intermediate", javaLibrary, dependencies = listOf(library))
    val test = createTarget("//aaa/bbb:test", javaTest, dependencies = listOf(intermediateLibrary))

    classify(listOf(test, intermediateLibrary, library)) shouldNotContain library
  }
}

private val baseDir = Path.of("projects", "testProject")
private val sourceRoot = baseDir.resolve("src")
private val resourceRoot = baseDir.resolve("resources")

private val javaLibrary = TargetKind("java_library", ruleType = RuleType.LIBRARY)
private val javaBinary = TargetKind("java_binary", ruleType = RuleType.BINARY)
private val javaTest = TargetKind("java_test", ruleType = RuleType.TEST)

private fun createTarget(
  id: String,
  kind: TargetKind,
  dependencies: List<RawBuildTarget> = emptyList(),
  sources: List<String> = emptyList(),
  resources: List<String> = emptyList(),
  isTestOnly: Boolean = false,
): RawBuildTarget {
  return RawBuildTarget(
    key = WorkspaceTargetKey(label = Label.parse(id)),
    dependencies = dependencies.map { DependencyLabel(it.key) },
    kind = kind,
    sources = SourceFileCollectionBuilder.build(relativeRoot = baseDir, paths = sources.map { sourceRoot.resolve(it) }),
    generatedSources = SourceFileCollection.EMPTY,
    resources = SourceFileCollectionBuilder.build(relativeRoot = baseDir, paths = resources.map { resourceRoot.resolve(it) }),
    baseDirectory = baseDir,
    isTestOnly = isTestOnly,
  )
}

private fun classify(targets: List<RawBuildTarget>): List<RawBuildTarget> {
  val testLabels = TestTargetClassifier.calculateTargetsToMarkAsTest(targets)
  return testLabels.mapNotNull { label -> targets.firstOrNull { it.id == label } }
}
