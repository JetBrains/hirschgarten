package org.jetbrains.bazel.workspace.indexing

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
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
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

class IndexableContentCollectorTest : WorkspaceModelBaseTest() {

  private val firstContributor = TestContributor()
  private val secondContributor = TestContributor()

  @BeforeEach
  fun maskRegisteredContributors() {
    ExtensionTestUtil.maskExtensions(IndexableContentContributor.ep, listOf(firstContributor, secondContributor), disposable)
  }

  @Test
  fun `should match default Bazel files`() {
    val collector = collector()

    collector.shouldBeIndexed(projectFile("foo.bzl")).shouldBeTrue()
    collector.shouldBeIndexed(projectFile("BUILD")).shouldBeTrue()
    collector.shouldBeIndexed(projectFile("BUILD.bazel")).shouldBeTrue()
    collector.shouldBeIndexed(projectFile("WORKSPACE")).shouldBeTrue()
    collector.shouldBeIndexed(projectFile("MODULE.bazel")).shouldBeTrue()
  }

  @Test
  fun `should not match unrelated files`() {
    val collector = collector()

    collector.shouldBeIndexed(projectFile("foo.txt")).shouldBeFalse()
    collector.shouldBeIndexed(projectFile("foo.md")).shouldBeFalse()
    collector.shouldBeIndexed(projectFile("foo.unknown")).shouldBeFalse()
  }

  @Test
  fun `should match custom index patterns`() {
    val collector = collector(listOf("*.custom"))

    collector.shouldBeIndexed(projectFile("foo.custom")).shouldBeTrue()
    collector.shouldBeIndexed(projectFile("nested/foo.custom")).shouldBeTrue()
    collector.shouldBeIndexed(projectFile("foo.txt")).shouldBeFalse()
  }

  @Test
  fun `should collect a directory pattern as an indexable root`() {
    val docs = projectFile("docs/readme.txt").parent
    projectFile("src/readme.txt")

    val content = collector(listOf("docs/*")).collect(virtualFileUrlManager)

    content.recursiveRoots.map { it.virtualFile }.shouldContainExactly(docs)
  }

  @Test
  fun `should collect the included root when all files are indexed`() {
    val content = collector(listOf("*")).collect(virtualFileUrlManager)

    content.recursiveRoots.map { it.virtualFile }.shouldContainExactly(project.rootDir)
  }

  @Test
  fun `should index root workspace files when the root is not included`() {
    val included = projectFile("included/BUILD").parent
    val workspace = projectFile("MODULE.bazel")
    val build = projectFile("BUILD")
    val collector = collector(includedRoots = setOf(included))

    collector.shouldBeIndexed(workspace).shouldBeTrue()
    collector.shouldBeIndexed(build).shouldBeFalse()
    val nonRecursiveRoots = collector.collect(virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile }
    nonRecursiveRoots shouldContain workspace
    nonRecursiveRoots shouldNotContain build
  }

  @Test
  fun `should index a target directory inside an excluded directory`() {
    val excludedBuild = projectFile("a/b/BUILD")
    val targetBuild = projectFile("a/b/c/BUILD")
    val collector = collector(includedRoots = setOf(project.rootDir, targetBuild.parent), excludedRoots = setOf(excludedBuild.parent))

    collector.shouldBeIndexed(targetBuild).shouldBeTrue()
    collector.shouldBeIndexed(excludedBuild).shouldBeFalse()
    val nonRecursiveRoots = collector.collect(virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile }
    nonRecursiveRoots shouldContain targetBuild
    nonRecursiveRoots shouldNotContain excludedBuild
  }

  @Test
  fun `should not index an excluded directory inside an included directory`() {
    val build = projectFile("a/b/BUILD")
    val collector = collector(includedRoots = setOf(project.rootDir), excludedRoots = setOf(build.parent))

    collector.shouldBeIndexed(build).shouldBeFalse()
    collector.collect(virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile } shouldNotContain build
  }

  @Test
  fun `should not index files under a content root`() {
    val build = projectFile("src/BUILD")
    val collector = collector(includedRoots = setOf(project.rootDir), contentRoots = setOf(build.parent))

    collector.shouldBeIndexed(build).shouldBeFalse()
    collector.collect(virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile } shouldNotContain build
  }

  @Test
  fun `should index a target directory inside an excluded directory under a content root`() {
    val content = projectFile("src/BUILD").parent
    val excluded = projectFile("src/gen/BUILD").parent
    val targetBuild = projectFile("src/gen/kept/BUILD")
    val collector = collector(
      includedRoots = setOf(project.rootDir, targetBuild.parent),
      excludedRoots = setOf(excluded),
      contentRoots = setOf(content),
    )

    collector.shouldBeIndexed(targetBuild).shouldBeTrue()
    collector.collect(virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile } shouldContain targetBuild
  }

  @Test
  fun `should not index an included directory under a content root`() {
    val content = projectFile("src/BUILD").parent
    val build = projectFile("src/x/BUILD")
    val collector = collector(includedRoots = setOf(build.parent), contentRoots = setOf(content))

    collector.shouldBeIndexed(build).shouldBeFalse()
    collector.collect(virtualFileUrlManager).nonRecursiveRoots.map { it.virtualFile } shouldNotContain build
  }

  @Test
  fun `should collect a target directory inside an excluded directory as a recursive root`() {
    val excluded = projectFile("a/b/BUILD").parent
    val targetBuild = projectFile("a/b/c/BUILD")
    val collector = collector(
      patterns = listOf("*"),
      includedRoots = setOf(project.rootDir, targetBuild.parent),
      excludedRoots = setOf(excluded),
    )

    collector.collect(virtualFileUrlManager).recursiveRoots.map { it.virtualFile }
      .shouldContainExactlyInAnyOrder(project.rootDir, targetBuild.parent)
  }

  @Test
  fun `should merge the content of all contributors`() {
    val workspace = projectFile("MODULE.bazel")
    val firstRecursiveRoot = projectFile("first/data.txt").parent
    val firstFile = projectFile("first.txt")
    val secondRecursiveRoot = projectFile("second/data.txt").parent
    val secondFile = projectFile("second.txt")
    firstContributor.content = IndexableContent(recursiveRoots = setOf(url(firstRecursiveRoot)), nonRecursiveRoots = setOf(url(firstFile)))
    secondContributor.content = IndexableContent(recursiveRoots = setOf(url(secondRecursiveRoot)), nonRecursiveRoots = setOf(url(secondFile)))

    val content = collector(includedRoots = emptySet()).collect(virtualFileUrlManager)

    content.recursiveRoots.map { it.virtualFile }.shouldContainExactlyInAnyOrder(firstRecursiveRoot, secondRecursiveRoot)
    content.nonRecursiveRoots.map { it.virtualFile }.shouldContainAll(workspace, firstFile, secondFile)
  }

  @Test
  fun `should select the same files in shouldBeIndexed and collect`() {
    val rootBuild = projectFile("BUILD")
    val rootNotes = projectFile("notes.txt")
    val contentBuild = projectFile("src/BUILD")
    val excludedUnderContentBuild = projectFile("src/gen/BUILD")
    val includedUnderContentBuild = projectFile("src/x/BUILD")
    val targetUnderContentBuild = projectFile("src/gen/kept/BUILD")
    val excludedBuild = projectFile("a/b/BUILD")
    val targetBuild = projectFile("a/b/c/BUILD")
    val targetNotes = projectFile("a/b/c/notes.txt")
    val includedBuild = projectFile("a/d/BUILD")
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

    val collected = collector.collect(virtualFileUrlManager).nonRecursiveRoots.mapNotNullTo(hashSetOf()) { it.virtualFile }

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

  private fun projectFile(relativePath: String): VirtualFile {
    val path = projectBasePath.resolve(relativePath)
    path.parent.createDirectories()
    path.createFile()
    return requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path))
  }

  private fun url(file: VirtualFile) = virtualFileUrlManager.storeAndGet(file)

  private class TestContributor : IndexableContentContributor {
    var content: IndexableContent = IndexableContent()

    override fun getIndexableContent(project: Project): IndexableContent = content
  }
}
