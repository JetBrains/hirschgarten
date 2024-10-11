package org.jetbrains.bsp.bazel.server.model

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

@DisplayName("Project tests")
class ProjectTest {
  @Nested
  @DisplayName("project1 + project tests")
  inner class ProjectPlus {
    @Test
    fun `should throw an exception if workspaceRoot differs`() {
      // given
      val project1 =
        Project(
          workspaceRoot = URI.create("file:///path/to/workspace"),
          modules = emptyList(),
          sourceToTarget = emptyMap(),
          libraries = emptyMap(),
          goLibraries = emptyMap(),
          invalidTargets = emptyList(),
          nonModuleTargets = emptyList(),
          bazelRelease = BazelRelease(0),
        )

      val project2 =
        Project(
          workspaceRoot = URI.create("file:///path/to/another/workspace"),
          modules = emptyList(),
          sourceToTarget = emptyMap(),
          libraries = emptyMap(),
          goLibraries = emptyMap(),
          invalidTargets = emptyList(),
          nonModuleTargets = emptyList(),
          bazelRelease = BazelRelease(0),
        )

      // then
      shouldThrowWithMessage<IllegalStateException>(
        "Cannot add projects with different workspace roots: file:///path/to/workspace and file:///path/to/another/workspace",
      ) {
        project1 + project2
      }
    }

    @Test
    fun `should throw an exception if bazelRelease differs`() {
      // given
      val project1 =
        Project(
          workspaceRoot = URI.create("file:///path/to/workspace"),
          modules = emptyList(),
          sourceToTarget = emptyMap(),
          libraries = emptyMap(),
          goLibraries = emptyMap(),
          invalidTargets = emptyList(),
          nonModuleTargets = emptyList(),
          bazelRelease = BazelRelease(21),
        )

      val project2 =
        Project(
          workspaceRoot = URI.create("file:///path/to/workspace"),
          modules = emptyList(),
          sourceToTarget = emptyMap(),
          libraries = emptyMap(),
          goLibraries = emptyMap(),
          invalidTargets = emptyList(),
          nonModuleTargets = emptyList(),
          bazelRelease = BazelRelease(37),
        )

      // then
      shouldThrowWithMessage<IllegalStateException>(
        "Cannot add projects with different bazel versions: BazelRelease(major=21) and BazelRelease(major=37)",
      ) {
        project1 + project2
      }
    }

    @Test
    fun `should add two projects`() {
      // given
      val project1 =
        Project(
          workspaceRoot = URI.create("file:///path/to/workspace"),
          modules = listOf("//project1:module1".toMockModule(), "//project1:module2".toMockModule(), "//module".toMockModule()),
          sourceToTarget =
            mapOf(
              URI.create("file:///project1/module1/file1.kt") to "//project1:module1".toLabel(),
              URI.create("file:///project1/module1/file2.kt") to "//project1:module1".toLabel(),
              URI.create("file:///project1/module2/file1.kt") to "//project1:module2".toLabel(),
            ),
          libraries =
            mapOf(
              "@library1//lib".toLabel() to "@library1//lib".toMockLibrary(),
              "@library2//lib".toLabel() to "@library2//lib".toMockLibrary(),
            ),
          goLibraries =
            mapOf(
              "@golibrary1//lib".toLabel() to "@golibrary1//lib".toMockGoLibrary(),
              "@golibrary2//lib".toLabel() to "@golibrary2//lib".toMockGoLibrary(),
            ),
          invalidTargets = listOf("//invalid1".toLabel()),
          nonModuleTargets = listOf("//nonmodule1".toMockNonModuleTarget(), "//nonmodule2".toMockNonModuleTarget()),
          bazelRelease = BazelRelease(1),
        )

      val project2 =
        Project(
          workspaceRoot = URI.create("file:///path/to/workspace"),
          modules =
            listOf(
              "//project2:module1".toMockModule(),
              "//project2:module2".toMockModule(),
              "//project2:module3".toMockModule(),
              "//module".toMockModule(),
            ),
          sourceToTarget =
            mapOf(
              URI.create("file:///project2/module1/file1.kt") to "//project2:module1".toLabel(),
              URI.create("file:///project2/module2/file1.kt") to "//project2:module2".toLabel(),
              URI.create("file:///project3/module3/file1.kt") to "//project2:module3".toLabel(),
            ),
          libraries =
            mapOf(
              "@library3//lib".toLabel() to "@library3//lib".toMockLibrary(),
              "@library4//lib".toLabel() to "@library4//lib".toMockLibrary(),
            ),
          goLibraries =
            mapOf(
              "@golibrary3//lib".toLabel() to "@golibrary3//lib".toMockGoLibrary(),
              "@golibrary4//lib".toLabel() to "@golibrary4//lib".toMockGoLibrary(),
            ),
          invalidTargets = listOf("//invalid2".toLabel()),
          nonModuleTargets = listOf("//nonmodule3".toMockNonModuleTarget()),
          bazelRelease = BazelRelease(1),
        )

      // then
      val expectedNewProject =
        Project(
          workspaceRoot = URI.create("file:///path/to/workspace"),
          modules =
            listOf(
              "//project1:module1".toMockModule(),
              "//project1:module2".toMockModule(),
              "//project2:module1".toMockModule(),
              "//project2:module2".toMockModule(),
              "//project2:module3".toMockModule(),
              "//module".toMockModule(),
            ),
          sourceToTarget =
            mapOf(
              URI.create("file:///project2/module1/file1.kt") to "//project2:module1".toLabel(),
              URI.create("file:///project2/module2/file1.kt") to "//project2:module2".toLabel(),
              URI.create("file:///project3/module3/file1.kt") to "//project2:module3".toLabel(),
              URI.create("file:///project1/module1/file1.kt") to "//project1:module1".toLabel(),
              URI.create("file:///project1/module1/file2.kt") to "//project1:module1".toLabel(),
              URI.create("file:///project1/module2/file1.kt") to "//project1:module2".toLabel(),
            ),
          libraries =
            mapOf(
              "@library1//lib".toLabel() to "@library1//lib".toMockLibrary(),
              "@library2//lib".toLabel() to "@library2//lib".toMockLibrary(),
              "@library3//lib".toLabel() to "@library3//lib".toMockLibrary(),
              "@library4//lib".toLabel() to "@library4//lib".toMockLibrary(),
            ),
          goLibraries =
            mapOf(
              "@golibrary1//lib".toLabel() to "@golibrary1//lib".toMockGoLibrary(),
              "@golibrary2//lib".toLabel() to "@golibrary2//lib".toMockGoLibrary(),
              "@golibrary3//lib".toLabel() to "@golibrary3//lib".toMockGoLibrary(),
              "@golibrary4//lib".toLabel() to "@golibrary4//lib".toMockGoLibrary(),
            ),
          invalidTargets = listOf("//invalid1".toLabel(), "//invalid2".toLabel()),
          nonModuleTargets =
            listOf(
              "//nonmodule3".toMockNonModuleTarget(),
              "//nonmodule1".toMockNonModuleTarget(),
              "//nonmodule2".toMockNonModuleTarget(),
            ),
          bazelRelease = BazelRelease(1),
        )
      val newProject = project1 + project2

      newProject.workspaceRoot shouldBe expectedNewProject.workspaceRoot
      newProject.modules shouldContainExactlyInAnyOrder expectedNewProject.modules
      newProject.sourceToTarget shouldBe expectedNewProject.sourceToTarget
      newProject.libraries shouldBe expectedNewProject.libraries
      newProject.goLibraries shouldBe expectedNewProject.goLibraries
      newProject.invalidTargets shouldContainExactlyInAnyOrder expectedNewProject.invalidTargets
      newProject.nonModuleTargets shouldContainExactlyInAnyOrder expectedNewProject.nonModuleTargets
      newProject.bazelRelease shouldBe expectedNewProject.bazelRelease
    }
  }

  private fun String.toMockModule(): Module =
    Module(
      label = this.toLabel(),
      isSynthetic = false,
      directDependencies = emptyList(),
      languages = emptySet(),
      tags = emptySet(),
      baseDirectory = URI.create("file:///path/to/$this"),
      sourceSet = SourceSet(emptySet(), emptySet(), emptySet()),
      resources = emptySet(),
      outputs = emptySet(),
      sourceDependencies = emptySet(),
      languageData = null,
      environmentVariables = emptyMap(),
    )

  private fun String.toMockLibrary(): Library =
    Library(
      label = this.toLabel(),
      outputs = emptySet(),
      sources = emptySet(),
      dependencies = emptyList(),
    )

  private fun String.toMockGoLibrary(): GoLibrary = GoLibrary(label = toLabel())

  private fun String.toMockNonModuleTarget(): NonModuleTarget =
    NonModuleTarget(
      label = this.toLabel(),
      languages = emptySet(),
      tags = emptySet(),
      baseDirectory = URI.create("file:///path/to/$this"),
    )

  private fun String.toLabel(): Label = Label.parse(this)
}
