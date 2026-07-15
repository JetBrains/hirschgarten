package com.intellij.bazel.test.integration

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.GitHubProject
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.project.ReusableLocalProjectInfo
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemProperty
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.replaceService
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import org.jetbrains.bazel.languages.projectview.ProjectViewToWorkspaceContextConverter
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.workspace.WorkspaceContextProvider
import org.jetbrains.bazel.workspace.model.test.framework.mockWorkspaceContext
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.hours

@ApprovalTestApplication
internal class WorkspaceImportApprovalTests {

  companion object {
    private val approvalTestCases = listOf(
      // simple projects
      ApprovalTestCase(WorkspaceImportApprovalTestCases.InSaneBazel, WorkspaceImportApprovalTestData("InSaneBazel")),
      ApprovalTestCase(WorkspaceImportApprovalTestCases.SimpleKotlinTest, WorkspaceImportApprovalTestData("SimpleKotlinCase")),
      ApprovalTestCase(WorkspaceImportApprovalTestCases.LocalPathOverrideTest, WorkspaceImportApprovalTestData("LocalPathOverrideTest")),
      ApprovalTestCase(WorkspaceImportApprovalTestCases.NonModuleTargetsTest, WorkspaceImportApprovalTestData("NonModuleTargetsTest")),
      ApprovalTestCase(
        project = WorkspaceImportApprovalTestCases.SimpleJavaTest,
        data = WorkspaceImportApprovalTestData(
          name = "SimpleJavaTest",
          onProjectInit = { projectDir ->
            setupRemoteJdk(projectDir, "21")
          },
        ),
      ),
      ApprovalTestCase(WorkspaceImportApprovalTestCases.SimpleScalaTest, WorkspaceImportApprovalTestData("SimpleScalaTest")),

      // community
      ApprovalTestCase(
        project = TestCase(
          ideInfo = IdeInfo.IdeaUltimate,
          projectInfo = GitHubProject.fromGithub(
            branchName = "master",
            commitHash = "4d58c711cc49a90b454a2353046ce8392243a1fb",
            repoRelativeUrl = "Jetbrains/intellij-community",
            isReusable = true,
          ),
        ),
        data = WorkspaceImportApprovalTestData(
          name = "JetbrainsIntellijCommunity",
          heavy = true,
          projectView = { projectDir -> projectDir.resolve("community.bazelproject") },
        ),
      ),

      ApprovalTestCase(
        project = TestCase(
          ideInfo = IdeInfo.IdeaUltimate,
          projectInfo = LocalProjectInfo(
            projectDir = Path.of(System.getProperty("bazel.workspace.approval.test.ultimate.path")),
            isReusable = true,
          ),
        ),
        data = object : WorkspaceImportApprovalTestData(
          name = "JetbrainsIntellijUltimate",
          projectView = { projectDir -> projectDir.resolve("ultimate.bazelproject") },
        ) {
          override val expectedWorkspaceModelFile: Path
            get() = Path.of(System.getProperty("bazel.workspace.approval.test.ultimate.golden"))
        },
      ),
    ) + discoverRedcodesTestCases()

    fun discoverRedcodesTestCases(): List<ApprovalTestCase> {
      val testNames = listOf(
        "alias_macro", "kotlin_compiler_plugin", "kotlin_resources", "source_roots_overlap",
        "annotation_processor", "kotlin_compiler_plugin_args", "local_repo_resources",
        "external_maven_dep_with_lock_jar_exclude", "kotlin_compiler_plugin_leaking_classpath", "shared_kotlin_java_package",
        "java_import_jar", "kotlin_module_internal_mangling", "shared_sources_green",
        "java_resources", "kotlin_resource_jars", "shared_sources_red",

        "strict_dependencies/java_strict_deps",
        "strict_dependencies/java_strict_deps_deep_multiverse",
        "strict_dependencies/java_strict_deps_kotlin",
        "strict_dependencies/java_strict_deps_multiverse",
        "strict_dependencies/java_strict_deps_proto",
      )

      return testNames.map { testName ->
        val path = BazelPathManager.testProjectsRoot.resolve("redcodes/$testName")
        ApprovalTestCase(
          project = TestCase(
            ideInfo = IdeInfo.IdeaUltimate,
            projectInfo = ReusableLocalProjectInfo(projectDir = path),
          ),
          data = WorkspaceImportApprovalTestData(
            name = path.name,
            onProjectInit = { projectDir ->
              setupRemoteJdk(projectDir, "21")
              projectDir.resolve(".bazelversion").writeText("8.7.0")
            },
          ),
        )
      }
    }

    val allApprovalTestCases = approvalTestCases.map { Named.of(it.data.name, it) }
    val allApprovalTestCasesWithoutUltimate = allApprovalTestCases.filter { it.name != "JetbrainsIntellijUltimate" }
    val ultimateApprovalTestCases = allApprovalTestCases.filter { it.name == "JetbrainsIntellijUltimate" }
    val selectedApprovalTestCases =
      if (System.getProperty("bazel.workspace.approval.test.only.ultimate").toBoolean()) ultimateApprovalTestCases
      else allApprovalTestCasesWithoutUltimate
  }

  @BeforeEach
  fun onBefore() {
    Assumptions.assumeTrue(!UsefulTestCase.IS_UNDER_TEAMCITY, "only for local runs")
  }

  @AfterEach
  fun onAfter() = timeoutRunBlocking {
    IdeStarterBaseProjectTest.killBazelProcesses()
    IdeStarterBaseProjectTest.killCefProcesses()
  }

  @ParameterizedTest(name = "{0}")
  @FieldSource("selectedApprovalTestCases")
  @SystemProperty("NO_FS_ROOTS_ACCESS_CHECK", "true")
  fun `test workspace model matches expected values`(testCase: ApprovalTestCase) = timeoutRunBlocking(timeout = 1.hours) {
    Assumptions.assumeTrue(!testCase.data.heavy || System.getProperty("bazel.workspace.approval.test.run.heavy").toBoolean())
    projectFixture()
    val projectDir = testCase.project.projectInfo.downloadAndUnpackProject() ?: fail { "cannot unpack project" }

    // prepare project
    val diskCache: Path =
      Path.of(System.getProperty("user.home"), ".cache", "approval-test-cache")
    testCase.data.onProjectInit(projectDir)
    projectDir.resolve(".bazelrc")
      .writeText(
        text = "\ncommon --disk_cache=${diskCache.absolutePathString()}\n",
        options = arrayOf(
          StandardOpenOption.CREATE, StandardOpenOption.APPEND,
        ),
      )

    val projectManager = ProjectManagerEx.getInstanceEx()
    val projectOpenTask = OpenProjectTask.build()
    val project = projectManager.newProjectAsync(projectDir, projectOpenTask)
    val projectView = testCase.data.projectView(projectDir)
    project.replaceService(
      serviceInterface = WorkspaceContextProvider::class.java,
      instance = object : WorkspaceContextProvider {
        override suspend fun computeWorkspaceContext(
          project: Project,
          bazelExecutable: Path,
        ): WorkspaceContext =
          if (projectView == null) {
            mockWorkspaceContext
          }
          else {
            val projectView = readAction { ProjectViewFactory.fromProjectViewContent(project, projectView.readText()) }
            ProjectViewToWorkspaceContextConverter.convert(project, projectView, bazelExecutable)
          }
      },
      parentDisposable = project,
    )
    BazelProjectFixtures.initializeBazelProject(project, projectDir)
    // prevent automatic sync
    // projectManager.openProjectAsync(projectDir, projectOpenTask.withProject(project)) ?: fail { "cannot open project" }
    try {
      doWorkspaceModelTest(project, testCase.data)
    }
    finally {
      projectManager.forceCloseProjectAsync(project, save = false)
    }
  }

  class ApprovalTestCase(val project: TestCase<*>, val data: WorkspaceImportApprovalTestData)

}
