package org.jetbrains.bazel.sync.workspace.projectTree

import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.BazelProcess
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.mockBazelProcessLauncher
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.orFallbackVersion
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.sync.workspace.projectTree.BazelRunnerSpyStubbingHelper.captureBazelCommandFromMock
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

class WorkspaceDirectoriesFromProjectViewTest : BasePlatformTestCase() {

  private lateinit var workspaceRoot: Path
  private lateinit var bazelRunner: BazelRunner
  private lateinit var dotBazelBspPath: Path

  override fun setUp() {
    super.setUp()
    workspaceRoot = createTempDirectory("workspace")
    initializeBazelProject(project, workspaceRoot)
    bazelRunner = createMockBazelRunner()
    dotBazelBspPath = workspaceRoot.resolve(".bazelbsp")
  }

  fun `test should add workspace root to included directories when there is no directories section`() {
    // GIVEN
    val projectViewContent =
      """
        derive_targets_from_directories: false
      """.trimIndent()

    // WHEN
    val result = runMapper(projectViewContent)

    // THEN
    assertSameElements(
      result.includedDirectories,
      listOf(workspaceRoot),
    )
  }

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

    assertSameElements(
      result.includedDirectories,
      listOf(includedDir),
    )

    assertSameElements(
      result.excludedDirectories,
      listOf(excludedDir, dotBazelBspPath)
    )
  }

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

    assertSameElements(
      result.includedDirectories,
      listOf(includedDir, pkg),
    )
  }

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

    assertSameElements(
      result.includedDirectories,
      listOf(includedDir),
    )
  }

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

    assertSameElements(
      result.includedDirectories,
      listOf(includedDir, pkg),
    )
    assertSameElements(
      result.excludedDirectories,
      listOf(subpkg, dotBazelBspPath),
    )
  }

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

    assertSameElements(
      result.includedDirectories,
      listOf(includedDir, subpkg)
    )
    assertSameElements(
      result.excludedDirectories,
      listOf(pkg, dotBazelBspPath)
    )
  }

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

    assertSameElements(
      result.includedDirectories,
      listOf(includedDir),
    )
    assertSameElements(
      result.excludedDirectories,
      listOf(pkg, subpkg, dotBazelBspPath),
    )
  }

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

    assertSameElements(
      result.includedDirectories,
      listOf(includedDir, pkg, subpkg)
    )
    assertSameElements(
      result.excludedDirectories,
      listOf(dotBazelBspPath),
    )
  }

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

    assertSameElements(
      result.includedDirectories,
      listOf(includedDir, pkg)
    )
    assertSameElements(
      result.excludedDirectories,
      listOf(subpkg, dotBazelBspPath)
    )
  }

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

    assertSameElements(
      result.includedDirectories,
      listOf(includedDir, pkg, subpkg)
    )
    assertSameElements(
      result.excludedDirectories,
      listOf(dotBazelBspPath)
    )
  }

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

    assertSameElements(
      result.includedDirectories,
      listOf(includedDir, pkg, subpkg),
    )
    assertSameElements(
      result.excludedDirectories,
      listOf(subdir, dotBazelBspPath)
    )
  }

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
    assertSameElements(
      result.includedDirectories,
      aspectTestingDir,
      workspaceRoot
    )

    // AND aspect should NOT be excluded because of 'directories: .'
    assertDoesntContain(
      result.excludedDirectories,
      aspectDir
    )
  }

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
    assertDoesntContain(
      result.includedDirectories,
      aspectsDir
    )
  }

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
    val result = runMapper(projectViewContent, createRepoMapping("community", "community+", workspaceRoot.resolve("community")))

    // THEN
    assertSameElements(
      result.includedDirectories,
      listOf(toolboxDir, communityKernelDir),
    )
  }

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
    val bazelCommand = captureBazelCommandFromMock(bazelRunner)
    assertNotEmpty(bazelCommand.options)
    assertDoesntContain(bazelCommand.options, extraToolchainsOption)
  }

  private fun runMapper(
    projectViewContent: String,
    repoMapping: RepoMapping = RepoMappingDisabled,
  ): WorkspaceDirectoriesResult {
    val projectView = ProjectViewFactory.from(project, projectViewContent, root = workspaceRoot)
    val owner = ModalTaskOwner.guess()
    val mapper = BspProjectMapper(workspaceRoot, bazelRunner, projectView, BazelPathsResolver(createBazelInfo()))
    return runWithModalProgressBlocking(
      owner,
      "Running Bazel Query",
      TaskCancellation.cancellable(),
    ) {
      mapper.workspaceDirectories(repoMapping, TaskGroupId.EMPTY.task(""))
    }
  }

  private fun createRepoMapping(apparentName: String, canonicalName: String, path: Path): BzlmodRepoMapping =
    BzlmodRepoMapping(
      canonicalRepoNameToLocalPath = mapOf(canonicalName to workspaceRoot.relativize(path)),
      apparentRepoNameToCanonicalName = mapOf("" to "", apparentName to canonicalName),
      canonicalRepoNameToPath = mapOf("" to workspaceRoot, canonicalName to path),
      nonLocalCanonicalRepoNames = setOf(),
    )

  private fun createBazelInfo(): BazelInfo =
    BazelInfo(
      execRoot = workspaceRoot.resolve("bazel-exec"),
      outputBase = workspaceRoot.resolve("bazel-out"),
      workspaceRoot = workspaceRoot,
      bazelBin = workspaceRoot.resolve("bazel-bin"),
      release = BazelRelease.fromReleaseString("release 9.0.0").orFallbackVersion(),
      true,
      false,
      emptyList(),
    )

  private fun createMockBazelRunner(): BazelRunner {
    val realRunner = BazelRunner(null, workspaceRoot, mockBazelProcessLauncher, Path("bazel"))
    val runner = spy(realRunner)

    fun mockBuildfilesOutput(): String {
      val root = workspaceRoot
      val lines = mutableListOf<String>()

      fun addIfExists(rel: String) {
        val p = root.resolve(rel)
        if (Files.exists(p)) lines += rel
      }

      addIfExists(".bazelbsp/aspects/BUILD")
      addIfExists("included/BUILD")
      addIfExists("excluded/BUILD")
      addIfExists("pkg/BUILD")
      addIfExists("pkg/subpkg/BUILD")

      return lines.joinToString("\n")
    }

    val result = mock(BazelProcessResult::class.java)
    `when`(result.isNotSuccess).thenReturn(false)
    `when`(result.stdout).thenAnswer { mockBuildfilesOutput().toByteArray() }
    `when`(result.stderr).thenReturn(ByteArray(0))

    val process = mock(BazelProcess::class.java)
    runBlocking {
      `when`(process.waitAndGetResult()).thenReturn(result)
    }

    BazelRunnerSpyStubbingHelper.stubRunBazelCommand(runner, process)

    return runner
  }
}
