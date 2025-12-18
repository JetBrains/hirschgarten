package org.jetbrains.bazel.sync.workspace.projectTree

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.jpsCompilation.utils.JPS_COMPILED_BASE_DIRECTORY
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewToWorkspaceContextConverter
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.Project
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

class WorkspaceDirectoriesFromProjectViewTest : BasePlatformTestCase() {

  private lateinit var workspaceRoot: Path
  private lateinit var bazelRunner: BazelRunner
  private lateinit var bspInfo: BspInfo
  private lateinit var mapper: BspProjectMapper

  override fun setUp() {
    super.setUp()
    workspaceRoot = createTempDirectory("workspace")
    bazelRunner = BazelRunner(null, workspaceRoot, null)
    bspInfo = object : BspInfo(workspaceRoot) {
      override val bazelBspDir: Path
        get() = workspaceRoot.resolve(".bazelbsp")
    }
    mapper = BspProjectMapper(bazelRunner, bspInfo)
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
    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val project = createFromProjectView(projectView)

    val result = mapper.workspaceDirectories(project)

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

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val project = createFromProjectView(projectView)

    val result = mapper.workspaceDirectories(project)

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

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val project = createFromProjectView(projectView)

    val result = mapper.workspaceDirectories(project)

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

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val project = createFromProjectView(projectView)

    val result = mapper.workspaceDirectories(project)

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

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val project = createFromProjectView(projectView)

    val result = mapper.workspaceDirectories(project)

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

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val project = createFromProjectView(projectView)

    val result = mapper.workspaceDirectories(project)

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

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val project = createFromProjectView(projectView)

    val result = mapper.workspaceDirectories(project)

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

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val project = createFromProjectView(projectView)

    val result = mapper.workspaceDirectories(project)

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

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val project = createFromProjectView(projectView)

    val result = mapper.workspaceDirectories(project)

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

    val projectView = ProjectView.fromProjectViewPsiFile(psiFile as ProjectViewPsiFile)
    val project = createFromProjectView(projectView)

    val result = mapper.workspaceDirectories(project)

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



  private fun createFromProjectView(projectView: ProjectView): Project {
    val workspaceContext = ProjectViewToWorkspaceContextConverter
      .convert(projectView, bspInfo.bazelBspDir, workspaceRoot)

    return createFakeProject(
      directories = workspaceContext.directories,
      targets = workspaceContext.targets
    )
  }

  private fun createFakeProject(
      directories: List<ExcludableValue<Path>> = emptyList(),
      targets: List<ExcludableValue<Label>> = emptyList()
  ): Project {
    val context = WorkspaceContext(
        targets = targets,
        directories = directories,
        buildFlags = emptyList(),
        syncFlags = emptyList(),
        debugFlags = emptyList(),
        bazelBinary = Path.of("bazel"),
        allowManualTargetsSync = false,
        dotBazelBspDirPath = bspInfo.bazelBspDir,
        importDepth = -1,
        enabledRules = emptyList(),
        ideJavaHomeOverride = Path.of("java_home"),
        shardSync = false,
        targetShardSize = 1000,
        shardingApproach = null,
        importRunConfigurations = emptyList(),
        gazelleTarget = null,
        indexAllFilesInDirectories = false,
        pythonCodeGeneratorRuleNames = emptyList(),
        importIjars = false,
        deriveInstrumentationFilterFromTargets = true,
        indexAdditionalFilesInDirectories = emptyList(),
        preferClassJarsOverSourcelessJars = true,
    )

    return AspectSyncProject(
        workspaceRoot = workspaceRoot,
        workspaceContext = context,
        repoMapping = RepoMappingDisabled,
        bazelRelease = BazelRelease(8),
        workspaceName = "test",
        targets = emptyMap(),
        rootTargets = emptySet()
    )
  }
}
