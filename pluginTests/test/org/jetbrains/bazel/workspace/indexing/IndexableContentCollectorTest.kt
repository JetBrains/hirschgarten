package org.jetbrains.bazel.workspace.indexing

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.storeAndGet
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.testFramework.ExtensionTestUtil
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.virtualFileUrlManager
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class IndexableContentCollectorTest {
  private val testBazel by testBazelProjectFixture()

  private val project get() = testBazel.project

  private val firstContributor = TestContributor()
  private val secondContributor = TestContributor()

  @BeforeEach
  fun maskRegisteredContributors() {
    ExtensionTestUtil.maskExtensions(IndexableContentContributor.ep, listOf(firstContributor, secondContributor), testBazel.disposable)
  }

  @Test
  fun `should match default Bazel files`() {
    val collector = collector()

    collector.shouldBeIndexed(testBazel.writeFile("foo.bzl")).shouldBeTrue()
    collector.shouldBeIndexed(testBazel.writeFile("BUILD")).shouldBeTrue()
    collector.shouldBeIndexed(testBazel.writeFile("BUILD.bazel")).shouldBeTrue()
    collector.shouldBeIndexed(testBazel.writeFile("WORKSPACE")).shouldBeTrue()
    collector.shouldBeIndexed(testBazel.writeFile("MODULE.bazel")).shouldBeTrue()
  }

  @Test
  fun `should not match unrelated files`() {
    val collector = collector()

    collector.shouldBeIndexed(testBazel.writeFile("foo.txt")).shouldBeFalse()
    collector.shouldBeIndexed(testBazel.writeFile("foo.md")).shouldBeFalse()
    collector.shouldBeIndexed(testBazel.writeFile("foo.unknown")).shouldBeFalse()
  }

  @Test
  fun `should match custom index patterns`() {
    val collector = collector(listOf("*.custom"))

    collector.shouldBeIndexed(testBazel.writeFile("foo.custom")).shouldBeTrue()
    collector.shouldBeIndexed(testBazel.writeFile("nested/foo.custom")).shouldBeTrue()
    collector.shouldBeIndexed(testBazel.writeFile("foo.txt")).shouldBeFalse()
  }

  @Test
  fun `should collect a directory pattern as an indexable root`() {
    val docs = testBazel.writeFile("docs/readme.txt").parent
    testBazel.writeFile("src/readme.txt")

    val content = collector(listOf("docs/*")).collect(testBazel.virtualFileUrlManager)

    content.recursiveRoots.map { it.virtualFile }.shouldContainExactly(docs)
  }

  @Test
  fun `should collect the included root when all files are indexed`() {
    val content = collector(listOf("*")).collect(testBazel.virtualFileUrlManager)

    content.recursiveRoots.map { it.virtualFile }.shouldContainExactly(project.rootDir)
  }

  @Test
  fun `should index root workspace files when the root is not included`() {
    val included = testBazel.writeFile("included/BUILD").parent
    val workspace = testBazel.writeFile("MODULE.bazel")
    val build = testBazel.writeFile("BUILD")
    val collector = collector(includedRoots = setOf(included))

    collector.shouldBeIndexed(workspace).shouldBeTrue()
    collector.shouldBeIndexed(build).shouldBeFalse()
    val nonRecursiveRoots = collector.collect(testBazel.virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile }
    nonRecursiveRoots shouldContain workspace
    nonRecursiveRoots shouldNotContain build
  }

  @Test
  fun `should index a target directory inside an excluded directory`() {
    val excludedBuild = testBazel.writeFile("a/b/BUILD")
    val targetBuild = testBazel.writeFile("a/b/c/BUILD")
    val collector = collector(includedRoots = setOf(project.rootDir, targetBuild.parent), excludedRoots = setOf(excludedBuild.parent))

    collector.shouldBeIndexed(targetBuild).shouldBeTrue()
    collector.shouldBeIndexed(excludedBuild).shouldBeFalse()
    val nonRecursiveRoots = collector.collect(testBazel.virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile }
    nonRecursiveRoots shouldContain targetBuild
    nonRecursiveRoots shouldNotContain excludedBuild
  }

  @Test
  fun `should not index an excluded directory inside an included directory`() {
    val build = testBazel.writeFile("a/b/BUILD")
    val collector = collector(includedRoots = setOf(project.rootDir), excludedRoots = setOf(build.parent))

    collector.shouldBeIndexed(build).shouldBeFalse()
    collector.collect(testBazel.virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile } shouldNotContain build
  }

  @Test
  fun `should not index files under a content root`() {
    val build = testBazel.writeFile("src/BUILD")
    val collector = collector(includedRoots = setOf(project.rootDir), contentRoots = setOf(build.parent))

    collector.shouldBeIndexed(build).shouldBeFalse()
    collector.collect(testBazel.virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile } shouldNotContain build
  }

  @Test
  fun `should index a target directory inside an excluded directory under a content root`() {
    val content = testBazel.writeFile("src/BUILD").parent
    val excluded = testBazel.writeFile("src/gen/BUILD").parent
    val targetBuild = testBazel.writeFile("src/gen/kept/BUILD")
    val collector = collector(
      includedRoots = setOf(project.rootDir, targetBuild.parent),
      excludedRoots = setOf(excluded),
      contentRoots = setOf(content),
    )

    collector.shouldBeIndexed(targetBuild).shouldBeTrue()
    collector.collect(testBazel.virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile } shouldContain targetBuild
  }

  @Test
  fun `should not index an included directory under a content root`() {
    val content = testBazel.writeFile("src/BUILD").parent
    val build = testBazel.writeFile("src/x/BUILD")
    val collector = collector(includedRoots = setOf(build.parent), contentRoots = setOf(content))

    collector.shouldBeIndexed(build).shouldBeFalse()
    collector.collect(testBazel.virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile } shouldNotContain build
  }

  @Test
  fun `should collect a target directory inside an excluded directory as a recursive root`() {
    val excluded = testBazel.writeFile("a/b/BUILD").parent
    val targetBuild = testBazel.writeFile("a/b/c/BUILD")
    val collector = collector(
      patterns = listOf("*"),
      includedRoots = setOf(project.rootDir, targetBuild.parent),
      excludedRoots = setOf(excluded),
    )

    collector.collect(testBazel.virtualFileUrlManager).recursiveRoots.map { it.virtualFile }
      .shouldContainExactlyInAnyOrder(project.rootDir, targetBuild.parent)
  }

  @Test
  fun `should merge the content of all contributors`() {
    val workspace = testBazel.writeFile("MODULE.bazel")
    val firstRecursiveRoot = testBazel.writeFile("first/data.txt").parent
    val firstFile = testBazel.writeFile("first.txt")
    val secondRecursiveRoot = testBazel.writeFile("second/data.txt").parent
    val secondFile = testBazel.writeFile("second.txt")
    firstContributor.content = IndexableContent(recursiveRoots = setOf(url(firstRecursiveRoot)), nonRecursiveRoots = setOf(url(firstFile)))
    secondContributor.content = IndexableContent(recursiveRoots = setOf(url(secondRecursiveRoot)), nonRecursiveRoots = setOf(url(secondFile)))

    val content = collector(includedRoots = emptySet()).collect(testBazel.virtualFileUrlManager)

    content.recursiveRoots.map { it.virtualFile }.shouldContainExactlyInAnyOrder(firstRecursiveRoot, secondRecursiveRoot)
    content.nonRecursiveRoots.map { it.virtualFile }.shouldContainAll(workspace, firstFile, secondFile)
  }

  @Test
  fun `should select the same files in shouldBeIndexed and collect`() {
    val rootBuild = testBazel.writeFile("BUILD")
    val rootNotes = testBazel.writeFile("notes.txt")
    val contentBuild = testBazel.writeFile("src/BUILD")
    val excludedUnderContentBuild = testBazel.writeFile("src/gen/BUILD")
    val includedUnderContentBuild = testBazel.writeFile("src/x/BUILD")
    val targetUnderContentBuild = testBazel.writeFile("src/gen/kept/BUILD")
    val excludedBuild = testBazel.writeFile("a/b/BUILD")
    val targetBuild = testBazel.writeFile("a/b/c/BUILD")
    val targetNotes = testBazel.writeFile("a/b/c/notes.txt")
    val includedBuild = testBazel.writeFile("a/d/BUILD")
    val files = listOf(
      rootBuild,
      rootNotes,
      contentBuild,
      excludedUnderContentBuild,
      includedUnderContentBuild,
      targetUnderContentBuild,
      excludedBuild,
      targetBuild,
      targetNotes,
      includedBuild,
    )
    val collector = collector(
      includedRoots = setOf(project.rootDir, includedUnderContentBuild.parent, targetUnderContentBuild.parent, targetBuild.parent),
      excludedRoots = setOf(excludedUnderContentBuild.parent, excludedBuild.parent),
      contentRoots = setOf(contentBuild.parent),
    )
    val expected = listOf(rootBuild, targetUnderContentBuild, targetBuild, includedBuild)

    val collected = collector.collect(testBazel.virtualFileUrlManager).nonRecursiveRoots.mapNotNullTo(hashSetOf()) { it.virtualFile }

    files.filter(collector::shouldBeIndexed).shouldContainExactlyInAnyOrder(expected)
    files.filter { it in collected }.shouldContainExactlyInAnyOrder(expected)
  }

  private fun collector(
    patterns: List<String> = emptyList(),
    includedRoots: Set<VirtualFile> = setOf(project.rootDir),
    excludedRoots: Set<VirtualFile> = emptySet(),
    contentRoots: Set<VirtualFile> = emptySet(),
  ): IndexableContentCollector =
    IndexableContentCollector(
      project = project,
      indexPatterns = patterns,
      includedRoots = includedRoots,
      excludedRoots = excludedRoots,
      contentRoots = contentRoots,
    )

  private fun url(file: VirtualFile) = testBazel.virtualFileUrlManager.storeAndGet(file)

  private class TestContributor : IndexableContentContributor {
    var content: IndexableContent = IndexableContent()

    override fun getIndexableContent(project: Project): IndexableContent = content
  }
}
