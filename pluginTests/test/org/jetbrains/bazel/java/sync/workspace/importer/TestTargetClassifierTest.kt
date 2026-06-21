package org.jetbrains.bazel.java.sync.workspace.importer

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.assumeResolved
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
    val sources = listOf("Test.java")
    val target = createTarget("//aaa/bbb:ccc", javaTest, sources = sources)
    val targetSet = setOf(target)

    classify(targetSet) shouldContain target
    classifyWithSelfReference(targetSet) shouldContain target
  }

  @Test
  fun `should mark test-only target`() {
    val sources = listOf("Library.java")
    val target = createTarget("//aaa/bbb:ccc", javaLibrary, sources = sources, isTestOnly = true)
    val targetSet = setOf(target)

    classify(targetSet) shouldContain target
    classifyWithSelfReference(targetSet) shouldContain target
  }

  @Test
  fun `should not mark binary target`() {
    val sources = listOf("Binary.java")
    val target = createTarget("//aaa/bbb:ccc", javaBinary, sources = sources)
    val targetSet = setOf(target)

    classify(targetSet) shouldNotContain target
    classifyWithSelfReference(targetSet) shouldNotContain target
  }

  @Test
  fun `should not mark library not related to any tests`() {
    val sources = listOf("Library.java")
    val library1 = createTarget("//aaa/bbb:library1", javaLibrary)
    val library2 = createTarget("//aaa/bbb:library2", javaLibrary, sources = sources)
    val library3 = createTarget("//aaa/bbb:library3", javaLibrary, resources = listOf("Resource.txt"))
    val targetSet = setOf(library1, library2, library3)

    val result1 = classify(targetSet)
    val result2 = classifyWithSelfReference(targetSet)

    result1 shouldNotContain library1
    result2 shouldNotContain library1
    result1 shouldNotContain library2
    result2 shouldNotContain library2
    result1 shouldNotContain library3
    result2 shouldNotContain library3
  }

  @Test
  fun `should not mark library used in a non-test target`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val binary = createTarget("//aaa/bbb:binary", javaBinary, dependencies = listOf(library), sources = listOf("Binary.java"))
    val test = createTarget("//aaa/bbb:test", javaTest, dependencies = listOf(library))
    val targetSet = setOf(binary, test, library)
    val executableTargets =
      mapOf(library.id.assumeResolved() to listOf(binary.id, test.id))

    classify(targetSet, executableTargets) shouldNotContain library
    classifyWithSelfReference(targetSet, executableTargets) shouldNotContain library
  }

  @Test
  fun `should not mark library used in test target that has its own sources`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val test = createTarget("//aaa/bbb:test", javaTest, dependencies = listOf(library), sources = listOf("Test.java"))
    val targetSet = setOf(test, library)
    val executableTargets =
      mapOf(library.id.assumeResolved() to listOf(test.id))

    classify(targetSet, executableTargets) shouldNotContain library
    classifyWithSelfReference(targetSet, executableTargets) shouldNotContain library
  }

  @Test
  fun `should mark library used in test target without own sources`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val test = createTarget("//aaa/bbb:test", javaTest, dependencies = listOf(library))
    val targetSet = setOf(test, library)
    val executableTargets =
      mapOf(library.id.assumeResolved() to listOf(test.id))

    classify(targetSet, executableTargets) shouldContain library
    classifyWithSelfReference(targetSet, executableTargets) shouldContain library
  }

  @Test
  fun `should mark library used in multiple test targets, some without own sources`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val test1 = createTarget("//aaa/bbb:test1", javaTest, dependencies = listOf(library))
    val test2 = createTarget("//aaa/bbb:test2", javaTest, dependencies = listOf(library), sources = listOf("Test.java"))
    val test3 = createTarget("//aaa/bbb:test3", javaTest, dependencies = listOf(library))
    val targetSet = setOf(test1, test2, test3, library)
    val executableTargets =
      mapOf(library.id.assumeResolved() to listOf(test1.id, test2.id, test3.id))

    classify(targetSet, executableTargets) shouldContain library
    classifyWithSelfReference(targetSet, executableTargets) shouldContain library
  }

  @Test
  fun `should not mark library used in test target transitively`() {
    val library = createTarget("//aaa/bbb:library", javaLibrary, sources = listOf("Library.java"))
    val intermediateLibrary = createTarget("//aaa/bbb:intermediate", javaLibrary, dependencies = listOf(library))
    val test = createTarget("//aaa/bbb:test", javaTest, dependencies = listOf(intermediateLibrary))
    val targetSet = setOf(test, intermediateLibrary, library)
    val executableTargets =
      mapOf(library.id.assumeResolved() to listOf(test.id))

    classify(targetSet, executableTargets) shouldNotContain library
    classifyWithSelfReference(targetSet, executableTargets) shouldNotContain library
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

private fun classify(targets: Set<RawBuildTarget>, executableTargets: Map<ResolvedLabel, List<Label>> = emptyMap()): List<RawBuildTarget> {
  val byId = targets.associateBy { it.id }
  val testLabels = TestTargetClassifier.calculateTargetsToMarkAsTest(targets, byId, executableTargets)
  return testLabels.mapNotNull { label -> targets.firstOrNull { it.id == label } }
}

// Targets sometimes have themselves mentioned among their executable targets - this should not break anything
private fun classifyWithSelfReference(
  targets: Set<RawBuildTarget>,
  executableTargets: Map<ResolvedLabel, List<Label>> = emptyMap(),
): List<RawBuildTarget> {
  val expandedExecutableMap = executableTargets.toMutableMap()
  for (target in targets) {
    val label = target.id.assumeResolved()
    val references = executableTargets.getOrDefault(label, emptyList()).toMutableSet()
    references.add(label)
    expandedExecutableMap[label] = references.toList()
  }
  return classify(targets, expandedExecutableMap)
}
