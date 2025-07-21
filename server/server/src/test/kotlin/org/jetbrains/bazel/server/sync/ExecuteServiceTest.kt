package org.jetbrains.bazel.server.sync

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.jetbrains.bazel.bazelrunner.BazelCommand
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bsp.managers.BazelBspCompilationManager
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.model.Tag
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.workspacecontext.provider.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.TestParams
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.mock
import kotlin.io.path.Path

class ExecuteServiceTest {
  private lateinit var executeService: ExecuteService
  private lateinit var mockCompilationManager: BazelBspCompilationManager
  private lateinit var mockProjectProvider: ProjectProvider
  private lateinit var mockBazelRunner: BazelRunner
  private lateinit var mockWorkspaceContextProvider: WorkspaceContextProvider
  private lateinit var mockBazelPathsResolver: BazelPathsResolver
  private lateinit var mockAdditionalBuildTargetsProvider: AdditionalAndroidBuildTargetsProvider

  @BeforeEach
  fun setup() {
    mockCompilationManager = mock(BazelBspCompilationManager::class.java)
    mockProjectProvider = mock(ProjectProvider::class.java)
    mockBazelRunner = mock(BazelRunner::class.java)
    mockWorkspaceContextProvider = mock(WorkspaceContextProvider::class.java)
    mockBazelPathsResolver = mock(BazelPathsResolver::class.java)
    mockAdditionalBuildTargetsProvider = mock(AdditionalAndroidBuildTargetsProvider::class.java)
    executeService =
      ExecuteService(
        compilationManager = mockCompilationManager,
        projectProvider = mockProjectProvider,
        bazelRunner = mockBazelRunner,
        workspaceContextProvider = mockWorkspaceContextProvider,
        bazelPathsResolver = mockBazelPathsResolver,
        additionalBuildTargetsProvider = mockAdditionalBuildTargetsProvider,
      )
  }

  @Test
  fun `extractSingleModule should extract single module successfully`() =
    runTest {
      // given
      val targetLabel = Label.parse("//src/test:my_test")
      val testParams =
        TestParams(
          targets = listOf(targetLabel),
          originId = "test-origin",
          coverage = true,
        )

      val mockModule =
        createMockModule(
          label = targetLabel,
          languages = setOf(LanguageClass.JAVA),
        )

      setupMocksForSuccessfulExecution(listOf(mockModule), listOf(targetLabel))

      // when
      val result = executeService.extractSingleModule(testParams)

      // then
      result.label shouldBe targetLabel
    }

  @Test
  fun `extractSingleModule should fail when multiple modules are found for single target`() =
    runTest {
      // given
      val targetLabel1 = Label.parse("//src/test:my_test")
      val targetLabel2 = Label.parse("//src/test:another_test")
      val testParams =
        TestParams(
          targets = listOf(targetLabel1, targetLabel2),
          originId = "test-origin",
          coverage = true,
        )

      val mockModule1 =
        createMockModule(
          label = targetLabel1,
          languages = setOf(LanguageClass.JAVA),
        )
      val mockModule2 =
        createMockModule(
          label = targetLabel2,
          languages = setOf(LanguageClass.JAVA),
        )

      setupMocksForModuleSelection(listOf(mockModule1, mockModule2), listOf(targetLabel1, targetLabel2))

      // when & then
      val exception =
        assertThrows<RuntimeException> {
          executeService.extractSingleModule(testParams)
        }
      exception.message shouldBe "More than one supported target found for $targetLabel1"
    }

  @Test
  fun `addCoverageOptions should add coverage options for label with path segments`() =
    runTest {
      // given
      val targetLabel = Label.parse("//src/test:my_test")
      val testParams =
        TestParams(
          targets = listOf(targetLabel),
          originId = "test-origin",
          coverage = true,
        )

      val mockModule =
        createMockModule(
          label = targetLabel,
          languages = setOf(LanguageClass.JAVA),
        )

      setupMocksForSuccessfulExecution(listOf(mockModule), listOf(targetLabel))

      val bazelTestCommand = BazelCommand.Test("/path/to/bazel_binary")
      executeService.addCoverageOptions(bazelTestCommand, mockModule)

      val size = bazelTestCommand.options.size
      bazelTestCommand.options[size - 2] shouldBe "--combined_report=lcov"
      bazelTestCommand.options[size - 1] shouldBe "--instrumentation_filter=^//src[/:]"
    }

  @Test
  fun `addCoverageOptions should add coverage options  for label without path segments`() =
    runTest {
      // given
      val targetLabel = Label.parse("//:my_test")
      val testParams =
        TestParams(
          targets = listOf(targetLabel),
          originId = "test-origin",
          coverage = true,
        )

      val mockModule =
        createMockModule(
          label = targetLabel,
          languages = setOf(LanguageClass.JAVA),
        )

      setupMocksForSuccessfulExecution(listOf(mockModule), listOf(targetLabel))

      val bazelTestCommand = BazelCommand.Test("/path/to/bazel_binary")
      executeService.addCoverageOptions(bazelTestCommand, mockModule)

      val size = bazelTestCommand.options.size
      bazelTestCommand.options[size - 2] shouldBe "--combined_report=lcov"
      bazelTestCommand.options[size - 1] shouldBe "--instrumentation_filter=[:]"
    }

  private suspend fun setupMocksForSuccessfulExecution(modules: List<Module>, targets: List<Label>) {
    setupMocksForModuleSelection(modules, targets)
  }

  private suspend fun setupMocksForModuleSelection(modules: List<Module>, targets: List<Label>) {
    val mockProject = mock(AspectSyncProject::class.java)
    Mockito.`when`(mockProjectProvider.get()).thenReturn(mockProject)
    Mockito.`when`(mockProject.modules).thenReturn(modules)
    modules.forEach {
      Mockito.`when`(mockProject.findModule(it.label)).thenReturn(it)
    }
  }

  private fun createMockModule(
    label: Label,
    languages: Set<LanguageClass> = setOf(LanguageClass.JAVA),
    tags: Set<Tag> = emptySet(),
    isSynthetic: Boolean = false,
  ): Module =
    Module(
      label = label,
      isSynthetic = isSynthetic,
      directDependencies = emptyList(),
      languages = languages,
      tags = tags,
      baseDirectory = Path("/test/path"),
      sources = emptyList(),
      resources = emptySet(),
      sourceDependencies = emptySet(),
      languageData = null,
      environmentVariables = emptyMap(),
      kindString = "java_test",
    )
}
