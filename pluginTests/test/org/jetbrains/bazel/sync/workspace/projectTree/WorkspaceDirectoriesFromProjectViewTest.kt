package org.jetbrains.bazel.sync.workspace.projectTree

import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.testFramework.runInEdtAndGet
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.bazelInfo
import org.jetbrains.bazel.test.framework.testproject.finishedProcess
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.testBazelRunner
import org.jetbrains.bazel.test.framework.testproject.testRepoMapping
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

@BazelTestApplication
class WorkspaceDirectoriesFromProjectViewTest {
  private val testBazel by testBazelProjectFixture()

  private val project get() = testBazel.project

  private val workspaceRoot: Path get() = testBazel.workspaceRoot
  private val dotBazelBspPath: Path get() = workspaceRoot.resolve(".bazelbsp")
  private val bazelCommands = mutableListOf<List<String>>()
  private val bazelRunner: BazelRunner by lazy {
    testBazelRunner(workspaceRoot) { command ->
      bazelCommands += command
      finishedProcess(stdout = mockBuildfilesOutput())
    }
  }

  @Test
  fun `test should add workspace root to included directories when there is no directories section`() {
    // GIVEN
    val projectViewContent =
      """
        derive_targets_from_directories: false
      """.trimIndent()

    // WHEN
    val result = runMapper(projectViewContent)

    // THEN
    result.includedDirectories shouldContainExactlyInAnyOrder listOf(workspaceRoot)
  }

  @Test
  fun `test should correctly identify included and excluded directories`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val excludedDir = workspaceRoot.resolve("excluded").createDirectories()
    includedDir.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          included
          -excluded
      """.trimIndent()

    val result = runMapper(projectViewContent)

    result.includedDirectories shouldContainExactlyInAnyOrder listOf(includedDir)

    result.excludedDirectories shouldContainExactlyInAnyOrder listOf(excludedDir, dotBazelBspPath)
  }

  @Test
  fun `test include target should override exclude directory when derive_targets_from_directories is false`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          included
          -pkg
        targets:
          //pkg:target
        derive_targets_from_directories: false
      """.trimIndent()

    val result = runMapper(projectViewContent)

    result.includedDirectories shouldContainExactlyInAnyOrder listOf(includedDir, pkg)
  }

  @Test
  fun `test include target should not override exclude directory when derive_targets_from_directories is true`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          included
          -pkg
        targets:
          //pkg:target
        derive_targets_from_directories: true
      """.trimIndent()

    val result = runMapper(projectViewContent)

    result.includedDirectories shouldContainExactlyInAnyOrder listOf(includedDir)
  }

  @Test
  fun `test include package by target should not include subpackages`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    pkg.resolve("subdir").createDirectories()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          included
        targets:
          //pkg:target
      """.trimIndent()

    val result = runMapper(projectViewContent)

    result.includedDirectories shouldContainExactlyInAnyOrder listOf(includedDir, pkg)
    result.excludedDirectories shouldContainExactlyInAnyOrder listOf(subpkg, dotBazelBspPath)
  }

  @Test
  fun `test should include subpackage by target even if its parent is excluded when derive_targets_from_directories is false`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    pkg.resolve("subdir").createDirectories()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          included
          -pkg
        targets:
          //pkg/subpkg:target
        derive_targets_from_directories: false
      """.trimIndent()

    val result = runMapper(projectViewContent)

    result.includedDirectories shouldContainExactlyInAnyOrder listOf(includedDir, subpkg)
    result.excludedDirectories shouldContainExactlyInAnyOrder listOf(pkg, dotBazelBspPath)
  }

  @Test
  fun `test should not include subpackage by target if its parent is excluded when derive_targets_from_directories is true`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    pkg.resolve("subdir").createDirectories()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          included
          -pkg
        targets:
          //pkg/subpkg:target
        derive_targets_from_directories: true
      """.trimIndent()

    val result = runMapper(projectViewContent)

    result.includedDirectories shouldContainExactlyInAnyOrder listOf(includedDir)
    result.excludedDirectories shouldContainExactlyInAnyOrder listOf(pkg, subpkg, dotBazelBspPath)
  }

  @Test
  fun `test should include all subpackages if target is recursive`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          included
        targets:
          //pkg/...
      """.trimIndent()

    val result = runMapper(projectViewContent)

    result.includedDirectories shouldContainExactlyInAnyOrder listOf(includedDir, pkg, subpkg)
    result.excludedDirectories shouldContainExactlyInAnyOrder listOf(dotBazelBspPath)
  }

  @Test
  fun `test should exclude directory form directories section even if it is included by recursive target when derive_targets_from_directories is true`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          included
          -pkg/subpkg
        targets:
          //pkg/...
        derive_targets_from_directories: true
      """.trimIndent()

    val result = runMapper(projectViewContent)

    result.includedDirectories shouldContainExactlyInAnyOrder listOf(includedDir, pkg)
    result.excludedDirectories shouldContainExactlyInAnyOrder listOf(subpkg, dotBazelBspPath)
  }

  @Test
  fun `test should not exclude directory form directories section if it is included by recursive target when derive_targets_from_directories is false`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          included
          -pkg/subpkg
        targets:
          //pkg/...
        derive_targets_from_directories: false
      """.trimIndent()

    val result = runMapper(projectViewContent)

    result.includedDirectories shouldContainExactlyInAnyOrder listOf(includedDir, pkg, subpkg)
    result.excludedDirectories shouldContainExactlyInAnyOrder listOf(dotBazelBspPath)
  }

  @Test
  fun `test should exclude non-bazel-package directory form directories section even if it is included by recursive target when derive_targets_from_directories is false`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    val subdir = pkg.resolve("subdir").createDirectories()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          included
          -pkg/subdir
        targets:
          //pkg/...
        derive_targets_from_directories: false
      """.trimIndent()

    val result = runMapper(projectViewContent)

    result.includedDirectories shouldContainExactlyInAnyOrder listOf(includedDir, pkg, subpkg)
    result.excludedDirectories shouldContainExactlyInAnyOrder listOf(subdir, dotBazelBspPath)
  }

  @Test
  fun `test root non-recursive target should not exclude other directories when root is included`() {
    // GIVEN
    val aspectDir = workspaceRoot.resolve("aspect").createDirectories()
    aspectDir.resolve("BUILD").createFile()
    val aspectTestingDir = aspectDir.resolve("testing").createDirectories()
    aspectTestingDir.resolve("BUILD").createFile()
    workspaceRoot.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories:
          .
        targets:
          //:clwb_tests
          //aspect/testing/...
        derive_targets_from_directories: false
      """.trimIndent()

    // WHEN
    val result = runMapper(projectViewContent)

    // THEN aspect/testing and workspace root should be included
    result.includedDirectories shouldContainExactlyInAnyOrder listOf(aspectTestingDir, workspaceRoot)

    // AND aspect should NOT be excluded because of 'directories: .'
    result.excludedDirectories shouldNotContain aspectDir
  }

  @Test
  fun `test should not include aspects directory when all directories are imported and targets are derived`() {
    // GIVEN
    val aspectsDir = workspaceRoot.resolve(".bazelbsp/aspects").createDirectories()
    aspectsDir.resolve("BUILD").createFile()

    val projectViewContent =
      """
        directories: .
        derive_targets_from_directories: true
      """.trimIndent()

    // WHEN
    val result = runMapper(projectViewContent)

    // THEN
    result.includedDirectories shouldNotContain aspectsDir
  }

  @Test
  fun `test should include target directory from mapped external repository`() {
    // GIVEN
    val toolboxDir = workspaceRoot.resolve("toolbox").createDirectories()
    val communityKernelDir = workspaceRoot.resolve("community/fleet/kernel").createDirectories()
    val projectViewContent =
      """
        directories:
          toolbox
        targets:
          @community//fleet/kernel:...
        derive_targets_from_directories: false
      """.trimIndent()

    // WHEN
    val result = runMapper(
      projectViewContent,
      testRepoMapping(testBazel.bazelInfo) { local("community+", "community", apparentName = "community") },
    )

    // THEN
    result.includedDirectories shouldContainExactlyInAnyOrder listOf(toolboxDir, communityKernelDir)
  }

  @Test
  fun `test should not pass build flags to bazel query command`() {
    // GIVEN
    val extraToolchainsOption = "--extra_toolchains=//some_directory/non_existing_toolchain:non_existing_toolchain"
    val projectViewContent =
      """
        directories:
          some_directory
        build_flags:
          $extraToolchainsOption
        derive_targets_from_directories: true
      """.trimIndent()

    // WHEN
    runMapper(projectViewContent)

    // THEN
    val bazelCommand = bazelCommands.single()
    val options = bazelCommand.subList(bazelCommand.indexOf("query") + 1, bazelCommand.lastIndex)
    options.shouldNotBeEmpty()
    options shouldNotContain extraToolchainsOption
  }

  private fun runMapper(
    projectViewContent: String,
    repoMapping: RepoMapping = RepoMappingDisabled,
  ): WorkspaceDirectoriesResult {
    val projectView = ProjectViewFactory.from(project, projectViewContent, root = workspaceRoot)
    val owner = ModalTaskOwner.guess()
    val mapper = BspProjectMapper(workspaceRoot, bazelRunner, projectView, BazelPathsResolver(testBazel.bazelInfo))
    // runWithModalProgressBlocking enters a modal state, and this needs the EDT.
    return runInEdtAndGet {
      runWithModalProgressBlocking(
        owner,
        "Running Bazel Query",
        TaskCancellation.cancellable(),
      ) {
        mapper.workspaceDirectories(repoMapping, TaskGroupId.EMPTY.task(""))
      }
    }
  }

  private fun mockBuildfilesOutput(): String {
    val lines = mutableListOf<String>()

    fun addIfExists(rel: String) {
      val p = workspaceRoot.resolve(rel)
      if (Files.exists(p)) lines += rel
    }

    addIfExists(".bazelbsp/aspects/BUILD")
    addIfExists("included/BUILD")
    addIfExists("excluded/BUILD")
    addIfExists("pkg/BUILD")
    addIfExists("pkg/subpkg/BUILD")

    return lines.joinToString("\n")
  }
}
