package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.vfs.VirtualFile
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class ResolveFileTargetToVirtualFileTest {
  private val myFixture by bazelCodeInsightFixture()

  private val project get() = myFixture.project

  private lateinit var buildFile: VirtualFile

  @BeforeEach
  fun setUp() {
    myFixture.addFileToProject("MODULE.bazel", "module(name = \"test\")")
    buildFile = myFixture.addFileToProject("pkg/BUILD.bazel", """java_library(name = "target")""").virtualFile
    myFixture.addFileToProject("pkg/file.bzl", "")
    myFixture.addFileToProject("pkg/subdir/nested.bzl", "")
  }

  @Test
  fun testSingleTargetFileReturnsFile() {
    val label = Label.parse("//pkg:file.bzl")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldNotBeNull()
    result.name shouldBe "file.bzl"
  }

  @Test
  fun testDirectoryTargetReturnsNull() {
    val label = Label.parse("//pkg:subdir")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  @Test
  fun testNonExistentFileReturnsNull() {
    val label = Label.parse("//pkg:nonexistent")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  @Test
  fun testNestedPathInSingleTargetReturnsFile() {
    val label = Label.parse("//pkg:subdir/nested.bzl")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldNotBeNull()
    result.name shouldBe "nested.bzl"
  }

  @Test
  fun testAmbiguousEmptyTargetForFilePathReturnsNull() {
    // //pkg/file.bzl treats "pkg/file.bzl" as package path, which doesn't exist as a directory
    val label = Label.parse("//pkg/file.bzl")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  @Test
  fun testRelativeSingleTargetFileReturnsFile() {
    val label = Label.parse(":file.bzl")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldNotBeNull()
    result.name shouldBe "file.bzl"
  }

  @Test
  fun testRelativeDirectoryTargetReturnsNull() {
    val label = Label.parse(":subdir")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  @Test
  fun testRelativeAmbiguousEmptyTargetForDirectoryReturnsNull() {
    val label = Label.parse("subdir")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  @Test
  fun testRelativeAmbiguousEmptyTargetReturnsFile() {
    val label = Label.parse("subdir/nested.bzl")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldNotBeNull()
    result.name shouldBe "nested.bzl"
  }

  @Test
  fun testAllTargetsLabelReturnNull() {
    val label = Label.parse("//...")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }


  @Test
  fun testRuleTargetWithNoFileReturnsNull() {
    val label = Label.parse("//pkg:target")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  @Test
  fun testRuleTargetWithCollidingFileReturnsFile() {
    myFixture.addFileToProject("pkg/target", "")
    val label = Label.parse("//pkg:target")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldNotBeNull()
    result.name shouldBe "target"
  }
}
