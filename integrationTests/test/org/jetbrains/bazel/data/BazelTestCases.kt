package org.jetbrains.bazel.data

import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.ide.starter.project.TestCaseTemplate
import org.jetbrains.bazel.test.compat.IntegrationTestCompat
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.div
import kotlin.io.path.exists

object IdeaBazelCases : BaseBazelCasesParametrized(BazelTestContext.IDEA) {
  val NonIndexableFilesAllTabSESplit = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
      commitHash = "90fcdecc4ea14ca4e453565e667f67d2cb27eb6e",
      branchName = "main",
      relativePath = "simpleMultiLanguageTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
    ),
  )

  val ProjectViewChange = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "1b07410678cfef0042e82ecfaadbbd9f34c1cd03",
      branchName = "main",
      relativePath = "projectViewChangeTest",
      configure = { context ->
        BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false)
        preCacheBazelisk(context)
      },
    ),
  )
}

object GoLandBazelCases : BaseBazelCasesParametrized(BazelTestContext.GOLAND)

object GoPluginBazelCases : BaseBazelCasesParametrized(BazelTestContext.IDEA_GO_PLUGIN)

object PyCharmBazelCases : BaseBazelCasesParametrized(BazelTestContext.PYCHARM)

fun preCacheBazelisk(context: IDETestContext) {
  val systemBazelisk = listOf("/opt/homebrew/bin/bazelisk", "/usr/local/bin/bazelisk")
    .map { Path.of(it) }
    .firstOrNull { it.exists() } ?: return
  val cacheDir = (context.paths.systemDir / "bazel-plugin").createDirectories()
  val target = cacheDir / "bazelisk"
  if (!target.exists()) {
    target.createSymbolicLinkPointingTo(systemBazelisk)
  }
}

open class BaseBazelCasesParametrized(val context: BazelTestContext) : TestCaseTemplate(context.getIdeInfo()) {
  fun withProject(project: IdeStarterBazelProject): TestCase<GitProjectInfo> =
    withBazelProject(
      GitProjectInfo(
        repositoryUrl = project.repositoryUrl,
        commitHash = project.revision,
        branchName = project.branch,
        projectHomeRelativePath = { root -> root.resolve(project.projectPath) },
        isReusable = false,
        configureProjectBeforeUse = project.configureProject,
      ),
    )

  protected fun withDefaults(
    repositoryUrl: String,
    commitHash: String,
    branchName: String,
    relativePath: String? = null,
    configure: (IDETestContext) -> Unit = { },
  ) = GitProjectInfo(
    repositoryUrl = repositoryUrl,
    commitHash = commitHash,
    branchName = branchName,
    projectHomeRelativePath = { p -> relativePath?.let { p.resolve(it) } ?: p },
    isReusable = false,
    configureProjectBeforeUse = configure,
  )

  protected fun <T : ProjectInfoSpec> withBazelProject(projectInfo: T): TestCase<T> =
    withProject(projectInfo)
      .let { IntegrationTestCompat.interceptTestCase(it, context.getIdeInfo()) }
}

class IdeStarterBazelProject internal constructor(
  val repositoryUrl: String,
  val revision: String,
  val branch: String,
  val projectPath: String,
  val configureProject: (IDETestContext) -> Unit,
) {
  init {
    require(FULL_GIT_REVISION.matches(revision)) {
      "IDE-Starter fixture revision must be an exact 40-character lowercase hexadecimal commit, got '$revision'"
    }
  }

  private companion object {
    val FULL_GIT_REVISION = Regex("[0-9a-f]{40}")
  }
}

fun simpleBazelProject(
  revision: String,
  path: String,
  repositoryUrl: String = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
  branch: String = "main",
  configureProject: (IDETestContext) -> Unit = BazelProjectConfigurer::configureProjectBeforeUse,
): IdeStarterBazelProject = IdeStarterBazelProject(
  repositoryUrl = repositoryUrl,
  revision = revision,
  branch = branch,
  projectPath = path,
  configureProject = configureProject,
)
