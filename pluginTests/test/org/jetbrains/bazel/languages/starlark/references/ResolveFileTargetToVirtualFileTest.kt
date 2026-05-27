package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.vfs.VirtualFile
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.BazelBasePlatformTestCase

class ResolveFileTargetToVirtualFileTest : BazelBasePlatformTestCase() {
  private lateinit var buildFile: VirtualFile

  override fun setUp() {
    super.setUp()
    myFixture.addFileToProject("MODULE.bazel", "module(name = \"test\")")
    buildFile = myFixture.addFileToProject("pkg/BUILD.bazel", """java_library(name = "target")""").virtualFile
    myFixture.addFileToProject("pkg/file.bzl", "")
    myFixture.addFileToProject("pkg/subdir/nested.bzl", "")
  }

  fun testSingleTargetFileReturnsFile() {
    val label = Label.parse("//pkg:file.bzl")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldNotBeNull()
    result.name shouldBe "file.bzl"
  }

  fun testDirectoryTargetReturnsNull() {
    val label = Label.parse("//pkg:subdir")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  fun testNonExistentFileReturnsNull() {
    val label = Label.parse("//pkg:nonexistent")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  fun testNestedPathInSingleTargetReturnsFile() {
    val label = Label.parse("//pkg:subdir/nested.bzl")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldNotBeNull()
    result.name shouldBe "nested.bzl"
  }

  fun testAmbiguousEmptyTargetForFilePathReturnsNull() {
    // //pkg/file.bzl treats "pkg/file.bzl" as package path, which doesn't exist as a directory
    val label = Label.parse("//pkg/file.bzl")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  fun testRelativeSingleTargetFileReturnsFile() {
    val label = Label.parse(":file.bzl")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldNotBeNull()
    result.name shouldBe "file.bzl"
  }

  fun testRelativeDirectoryTargetReturnsNull() {
    val label = Label.parse(":subdir")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  fun testRelativeAmbiguousEmptyTargetForDirectoryReturnsNull() {
    val label = Label.parse("subdir")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  fun testRelativeAmbiguousEmptyTargetReturnsFile() {
    val label = Label.parse("subdir/nested.bzl")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldNotBeNull()
    result.name shouldBe "nested.bzl"
  }

  fun testAllTargetsLabelReturnNull() {
    val label = Label.parse("//...")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }


  fun testRuleTargetWithNoFileReturnsNull() {
    val label = Label.parse("//pkg:target")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldBeNull()
  }

  fun testRuleTargetWithCollidingFileReturnsFile() {
    myFixture.addFileToProject("pkg/target", "")
    val label = Label.parse("//pkg:target")
    val result = resolveFileTargetToVirtualFile(project, label, buildFile)
    result.shouldNotBeNull()
    result.name shouldBe "target"
  }
}
