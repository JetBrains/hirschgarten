package org.jetbrains.bazel.sync.workspace.projectTree

import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.BazelProcess
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.jpsCompilation.utils.JPS_COMPILED_BASE_DIRECTORY
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewToWorkspaceContextConverter
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.sync.workspace.projectTree.BazelRunnerSpyStubbingHelper.captureBazelCommandFromMock
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

class WorkspaceDirectoriesFromProjectViewTest : BasePlatformTestCase() {

  private lateinit var workspaceRoot: Path
  private lateinit var bazelRunner: BazelRunner
  private lateinit var bspInfo: BspInfo

  override fun setUp() {
    super.setUp()
    workspaceRoot = createTempDirectory("workspace")
    bazelRunner = createMockBazelRunner()
    bspInfo = object : BspInfo(workspaceRoot) {
      override val bazelBspDir: Path
        get() = workspaceRoot.resolve(".bazelbsp")
    }
  }

  fun `test should correctly identify included and excluded directories`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val excludedDir = workspaceRoot.resolve("excluded").createDirectories()
    includedDir.resolve("BUILD").createFile()

    val psiFile = myFixture.configureByText(
      "B.bazelproject",
      """
        directories:
          included
          -excluded
      """.trimIndent(),
    )

    val result = runMapper(psiFile)

    assertSameElements(
      result.includedDirectories.map { it.uri },
      listOf(includedDir.toUri().toString())
    )

    assertSameElements(
      result.excludedDirectories.map { it.uri },
      listOf(
        excludedDir.toUri().toString(),
        bspInfo.bazelBspDir.toUri().toString(),
        workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY).toUri().toString()
      )
    )
  }

  fun `test include target should override exclude directory when derive_targets_from_directories is false`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()

    val psiFile = myFixture.configureByText(
      "Targets.bazelproject",
      """
        directories:
          included
          -pkg
        targets:
          //pkg:target
        derive_targets_from_directories: false
      """.trimIndent()
    )

    val result = runMapper(psiFile)

    assertSameElements(
      result.includedDirectories.map { it.uri },
      listOf(
        includedDir.toUri().toString(),
        pkg.toUri().toString()
      )
    )
  }

  fun `test include target should not override exclude directory when derive_targets_from_directories is true`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()

    val psiFile = myFixture.configureByText(
      "Targets.bazelproject",
      """
        directories:
          included
          -pkg
        targets:
          //pkg:target
        derive_targets_from_directories: true
      """.trimIndent()
    )

    val result = runMapper(psiFile)

    assertSameElements(
      result.includedDirectories.map { it.uri },
      listOf(includedDir.toUri().toString())
    )
  }

  fun `test include package by target should not include subpackages`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    pkg.resolve("subdir").createDirectories()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val psiFile = myFixture.configureByText(
      "Targets.bazelproject",
      """
        directories:
          included
        targets:
          //pkg:target
      """.trimIndent()
    )

    val result = runMapper(psiFile)

    assertSameElements(
      result.includedDirectories.map { it.uri },
      listOf(
        includedDir.toUri().toString(),
        pkg.toUri().toString(),
      )
    )
    assertSameElements(
      result.excludedDirectories.map { it.uri },
      listOf(
        subpkg.toUri().toString(),
        bspInfo.bazelBspDir.toUri().toString(),
        workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY).toUri().toString(),
      )
    )
  }

  fun `test should include subpackage by target even if its parent is excluded when derive_targets_from_directories is false`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    pkg.resolve("subdir").createDirectories()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val psiFile = myFixture.configureByText(
      "Targets.bazelproject",
      """
        directories:
          included
          -pkg
        targets:
          //pkg/subpkg:target
        derive_targets_from_directories: false
      """.trimIndent()
    )

    val result = runMapper(psiFile)

    assertSameElements(
      result.includedDirectories.map { it.uri },
      listOf(
        includedDir.toUri().toString(),
        subpkg.toUri().toString(),
      )
    )
    assertSameElements(
      result.excludedDirectories.map { it.uri },
      listOf(
        pkg.toUri().toString(),
        bspInfo.bazelBspDir.toUri().toString(),
        workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY).toUri().toString(),
      )
    )
  }

  fun `test should not include subpackage by target if its parent is excluded when derive_targets_from_directories is true`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    pkg.resolve("subdir").createDirectories()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val psiFile = myFixture.configureByText(
      "Targets.bazelproject",
      """
        directories:
          included
          -pkg
        targets:
          //pkg/subpkg:target
        derive_targets_from_directories: true
      """.trimIndent()
    )

    val result = runMapper(psiFile)

    assertSameElements(
      result.includedDirectories.map { it.uri },
      listOf(
        includedDir.toUri().toString(),
      )
    )
    assertSameElements(
      result.excludedDirectories.map { it.uri },
      listOf(
        pkg.toUri().toString(),
        subpkg.toUri().toString(),
        bspInfo.bazelBspDir.toUri().toString(),
        workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY).toUri().toString(),
      )
    )
  }

  fun `test should include all subpackages if target is recursive`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val psiFile = myFixture.configureByText(
      "Targets.bazelproject",
      """
        directories:
          included
        targets:
          //pkg/...
      """.trimIndent()
    )

    val result = runMapper(psiFile)

    assertSameElements(
      result.includedDirectories.map { it.uri },
      listOf(
        includedDir.toUri().toString(),
        pkg.toUri().toString(),
        subpkg.toUri().toString(),
      )
    )
    assertSameElements(
      result.excludedDirectories.map { it.uri },
      listOf(
        bspInfo.bazelBspDir.toUri().toString(),
        workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY).toUri().toString(),
      )
    )
  }

  fun `test should exclude directory form directories section even if it is included by recursive target when derive_targets_from_directories is true`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val psiFile = myFixture.configureByText(
      "Targets.bazelproject",
      """
        directories:
          included
          -pkg/subpkg
        targets:
          //pkg/...
        derive_targets_from_directories: true
      """.trimIndent()
    )

    val result = runMapper(psiFile)

    assertSameElements(
      result.includedDirectories.map { it.uri },
      listOf(
        includedDir.toUri().toString(),
        pkg.toUri().toString(),
      )
    )
    assertSameElements(
      result.excludedDirectories.map { it.uri },
      listOf(
        subpkg.toUri().toString(),
        bspInfo.bazelBspDir.toUri().toString(),
        workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY).toUri().toString(),
      )
    )
  }

  fun `test should not exclude directory form directories section if it is included by recursive target when derive_targets_from_directories is false`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val psiFile = myFixture.configureByText(
      "Targets.bazelproject",
      """
        directories:
          included
          -pkg/subpkg
        targets:
          //pkg/...
        derive_targets_from_directories: false
      """.trimIndent()
    )

    val result = runMapper(psiFile)

    assertSameElements(
      result.includedDirectories.map { it.uri },
      listOf(
        includedDir.toUri().toString(),
        pkg.toUri().toString(),
        subpkg.toUri().toString(),
      )
    )
    assertSameElements(
      result.excludedDirectories.map { it.uri },
      listOf(
        bspInfo.bazelBspDir.toUri().toString(),
        workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY).toUri().toString(),
      )
    )
  }

  fun `test should exclude non-bazel-package directory form directories section even if it is included by recursive target when derive_targets_from_directories is false`() {
    val includedDir = workspaceRoot.resolve("included").createDirectories()
    val pkg = workspaceRoot.resolve("pkg").createDirectories()
    pkg.resolve("BUILD").createFile()
    val subdir = pkg.resolve("subdir").createDirectories()
    val subpkg = pkg.resolve("subpkg").createDirectories()
    subpkg.resolve("BUILD").createFile()

    val psiFile = myFixture.configureByText(
      "Targets.bazelproject",
      """
        directories:
          included
          -pkg/subdir
        targets:
          //pkg/...
        derive_targets_from_directories: false
      """.trimIndent()
    )

    val result = runMapper(psiFile)

    assertSameElements(
      result.includedDirectories.map { it.uri },
      listOf(
        includedDir.toUri().toString(),
        pkg.toUri().toString(),
        subpkg.toUri().toString(),
      )
    )
    assertSameElements(
      result.excludedDirectories.map { it.uri },
      listOf(
        subdir.toUri().toString(),
        bspInfo.bazelBspDir.toUri().toString(),
        workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY).toUri().toString(),
      )
    )
  }

  fun `test should not pass build flags to bazel query command`() {
    // GIVEN
    val extraToolchainsOption = "--extra_toolchains=//some_directory/non_existing_toolchain:non_existing_toolchain"
    val psiFile = myFixture.configureByText(
      "testeest.bazelproject",
      """
        directories:
          some_directory
        build_flags:
          $extraToolchainsOption
        derive_targets_from_directories: true
      """.trimIndent(),
    )

    // WHEN
    runMapper(psiFile)

    // THEN
    val bazelCommand = captureBazelCommandFromMock(bazelRunner)
    assertNotEmpty(bazelCommand.options)
    assertDoesntContain(bazelCommand.options, extraToolchainsOption)
  }

  private fun runMapper(psiFile: PsiFile?): WorkspaceDirectoriesResult {
    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val workspaceContext = ProjectViewToWorkspaceContextConverter
      .convert(projectView, workspaceRoot)
    val owner = ModalTaskOwner.guess()
    val mapper = BspProjectMapper(bazelRunner, bspInfo, workspaceContext)
    return runWithModalProgressBlocking(
      owner,
      "Running Bazel Query",
      TaskCancellation.cancellable()
    ) {
      mapper.workspaceDirectories(workspaceRoot)
    }
  }

  private fun createMockBazelRunner(): BazelRunner {
    val realRunner = BazelRunner(null, workspaceRoot)
    val runner = spy(realRunner)

    fun mockBuildfilesOutput(): String {
      val root = workspaceRoot
      val lines = mutableListOf<String>()

      fun addIfExists(rel: String) {
        val p = root.resolve(rel)
        if (Files.exists(p)) lines += rel
      }

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
