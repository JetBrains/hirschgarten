package org.jetbrains.bazel.server.sync

import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.jetbrains.bazel.bazelrunner.BazelCommand
import org.jetbrains.bazel.bazelrunner.BazelProcess
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.FileUtil
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bsp.managers.BazelBspCompilationManager
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.model.Tag
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.startup.FileUtilIntellij
import org.jetbrains.bazel.startup.IntellijEnvironmentProvider
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.provider.DefaultWorkspaceContextProvider
import org.jetbrains.bazel.workspacecontext.provider.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.TestParams
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory

class ExecuteServiceTest {
  private lateinit var executeService: ExecuteService
  private lateinit var mockCompilationManager: BazelBspCompilationManager
  private lateinit var mockProjectProvider: ProjectProvider
  private lateinit var mockBazelRunner: BazelRunner
  private lateinit var mockWorkspaceContextProvider: WorkspaceContextProvider
  private lateinit var mockBazelPathsResolver: BazelPathsResolver
  private lateinit var mockAdditionalBuildTargetsProvider: AdditionalAndroidBuildTargetsProvider
  private lateinit var workspaceRoot: Path
  private lateinit var projectViewFile: Path
  private lateinit var dotBazelBspDirPath: Path
  private lateinit var workspaceContext: WorkspaceContext

  @BeforeEach
  fun setup() {
    mockCompilationManager = mock(BazelBspCompilationManager::class.java)
    mockProjectProvider = mock(ProjectProvider::class.java)
    mockBazelRunner = mock(BazelRunner::class.java)
    mockWorkspaceContextProvider = mock(WorkspaceContextProvider::class.java)
    mockBazelPathsResolver = mock(BazelPathsResolver::class.java)
    mockAdditionalBuildTargetsProvider = mock(AdditionalAndroidBuildTargetsProvider::class.java)
    executeService =
      Mockito.spy(
        ExecuteService(
          compilationManager = mockCompilationManager,
          projectProvider = mockProjectProvider,
          bazelRunner = mockBazelRunner,
          workspaceContextProvider = mockWorkspaceContextProvider,
          bazelPathsResolver = mockBazelPathsResolver,
          additionalBuildTargetsProvider = mockAdditionalBuildTargetsProvider,
        ),
      )
    SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
    FileUtil.provideFileUtil(FileUtilIntellij)
    EnvironmentProvider.provideEnvironmentProvider(IntellijEnvironmentProvider)

    workspaceRoot = createTempDirectory("workspaceRoot")
    projectViewFile = workspaceRoot.resolve("projectview.bazelproject")
    dotBazelBspDirPath = workspaceRoot.resolve(".bazelbsp")
    workspaceContext =
      DefaultWorkspaceContextProvider(workspaceRoot, projectViewFile, dotBazelBspDirPath, FeatureFlags())
        .readWorkspaceContext()

    Mockito.`when`(mockCompilationManager.workspaceRoot).thenReturn(workspaceRoot)
    Mockito.`when`(mockCompilationManager.client).thenReturn(mock(JoinedBuildClient::class.java))
    Mockito.`when`(mockWorkspaceContextProvider.readWorkspaceContext()).thenReturn(workspaceContext)
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
      val coverageCommand = BazelCommand.Coverage("/path/to/bazel_binary")
      setupMocksForSuccessfulExecution(listOf(mockModule), listOf(targetLabel), coverageCommand, testParams.originId)

      // when
      val result = executeService.testWithDebug(testParams)

      // then
      result.statusCode shouldBe BazelStatus.SUCCESS
      result.originId shouldBe testParams.originId
      "--combined_report=lcov" shouldBeIn coverageCommand.options
      "--instrumentation_filter=^//src[/:]" shouldBeIn coverageCommand.options
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
          executeService.testWithDebug(testParams)
        }
      exception.message shouldBe "More than one supported target found for $targetLabel1"
    }

  @Test
  fun `addCoverageOptions should add coverage options for label with path segments`() =
    runTest {
      // given
      val targetLabel = Label.parse("//my-package/path/test:my_test")
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

      val coverageCommand = BazelCommand.Coverage("/path/to/bazel_binary")
      setupMocksForSuccessfulExecution(listOf(mockModule), listOf(targetLabel), coverageCommand, testParams.originId)

      // when
      val result = executeService.testWithDebug(testParams)

      // then
      result.statusCode shouldBe BazelStatus.SUCCESS
      result.originId shouldBe testParams.originId
      "--combined_report=lcov" shouldBeIn coverageCommand.options
      "--instrumentation_filter=^//my-package[/:]" shouldBeIn coverageCommand.options
    }

  @Test
  fun `addCoverageOptions should add coverage options for label without path segments`() =
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

      val coverageCommand = BazelCommand.Coverage("/path/to/bazel_binary")
      setupMocksForSuccessfulExecution(listOf(mockModule), listOf(targetLabel), coverageCommand, testParams.originId)

      val result = executeService.testWithDebug(testParams)

      // then
      result.statusCode shouldBe BazelStatus.SUCCESS
      result.originId shouldBe testParams.originId
      "--combined_report=lcov" shouldBeIn coverageCommand.options
      "--instrumentation_filter=[:]" shouldBeIn coverageCommand.options
    }

  private suspend fun setupMocksForSuccessfulExecution(
    modules: List<Module>,
    targets: List<Label>,
    command: BazelCommand,
    originId: String,
  ) {
    setupMocksForModuleSelection(modules, targets)
    val mockBazelProcess = mock(BazelProcess::class.java)
    Mockito.`when`(mockBazelRunner.buildBazelCommand(eq(workspaceContext), anyOrNull(), any())).thenReturn(command)
    Mockito
      .`when`(
        mockBazelRunner.runBazelCommand(
          eq(command),
          eq(originId),
          any(),
          any(),
          any(),
          anyOrNull(),
        ),
      ).thenReturn(mockBazelProcess)
    val mockBazelProcessResult = mock(BazelProcessResult::class.java)
    Mockito.`when`(mockBazelProcess.waitAndGetResult(eq(true))).thenReturn(mockBazelProcessResult)
    Mockito.`when`(mockBazelProcessResult.bazelStatus).thenReturn(BazelStatus.SUCCESS)
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
