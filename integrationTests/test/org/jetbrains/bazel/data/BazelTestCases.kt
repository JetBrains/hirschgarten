package org.jetbrains.bazel.data

import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.ide.starter.project.ReusableLocalProjectInfo
import com.intellij.ide.starter.project.TestCaseTemplate
import org.jetbrains.bazel.test.compat.IntegrationTestCompat
import org.jetbrains.bazel.test.framework.BazelPathManager
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.div
import kotlin.io.path.exists

object IdeaBazelCases : BaseBazelCasesParametrized(BazelTestContext.IDEA) {
  val NonIndexableFilesAllTabSESplit = withBazelProject(
    projectInfo = simpleBazelProject(
      path = "simpleMultiLanguageTest",
      configureProject = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
    ),
  )

  val ProjectViewChange = withBazelProject(
    projectInfo = simpleBazelProject(
      path = "projectViewChangeTest",
      configureProject = { context ->
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
  fun withProject(project: IdeStarterBazelProject): TestCase<IdeStarterBazelProject> = withBazelProject(project)

  protected fun <T : ProjectInfoSpec> withBazelProject(projectInfo: T): TestCase<T> =
    withProject(projectInfo)
      .let { IntegrationTestCompat.interceptTestCase(it, context.getIdeInfo()) }
}

class IdeStarterBazelProject internal constructor(
  path: String,
  override val configureProjectBeforeUse: (IDETestContext) -> Unit,
  testProjectsRoot: Path = BazelPathManager.bazelTestProjectsRoot,
) : ProjectInfoSpec {
  internal val fixtureRoot: Path
  internal val pathWithinFixture: Path?
  private val localProject: ReusableLocalProjectInfo

  init {
    require(path.isNotBlank()) { "Bazel test fixture path must not be empty" }
    require(!isAbsoluteOnAnyPlatform(path)) { "Bazel test fixture path must be relative: '$path'" }

    val projectPath = Path.of(path)
    require(projectPath.none { it.toString() == ".." } && path.split('/', '\\').none { it == ".." }) {
      "Bazel test fixture path must not contain '..': '$path'"
    }

    val fixtureName = projectPath.first().toString()
    require(fixtureName != ".") { "Bazel test fixture path must start with a fixture name: '$path'" }

    fixtureRoot = testProjectsRoot.resolve(fixtureName)
    pathWithinFixture = if (projectPath.nameCount == 1) null else projectPath.subpath(1, projectPath.nameCount).normalize()
    localProject = ReusableLocalProjectInfo(
      projectDir = fixtureRoot,
      configureProjectBeforeUse = configureProjectBeforeUse,
      description = "Bundled Bazel test fixture '$path'",
    )
  }

  override val isReusable: Boolean
    get() = localProject.isReusable

  override val downloadTimeout
    get() = localProject.downloadTimeout

  override fun downloadAndUnpackProject(): Path? {
    val copiedFixtureRoot = localProject.downloadAndUnpackProject() ?: return null
    val innerPath = pathWithinFixture ?: return copiedFixtureRoot
    val projectHome = copiedFixtureRoot.resolve(innerPath).normalize()
    require(projectHome.startsWith(copiedFixtureRoot) && projectHome.exists()) {
      "Bazel test fixture path does not exist: '$innerPath' in '$fixtureRoot'"
    }
    return projectHome
  }

  override fun getDescription(): String = localProject.getDescription()

  private companion object {
    val WINDOWS_ABSOLUTE_PATH = Regex("^[A-Za-z]:[\\\\/].*")

    fun isAbsoluteOnAnyPlatform(path: String): Boolean =
      Path.of(path).isAbsolute || WINDOWS_ABSOLUTE_PATH.matches(path) || path.startsWith("/") || path.startsWith("\\")
  }
}

fun simpleBazelProject(
  path: String,
  configureProject: (IDETestContext) -> Unit = BazelProjectConfigurer::configureProjectBeforeUse,
): IdeStarterBazelProject = IdeStarterBazelProject(
  path = path,
  configureProjectBeforeUse = configureProject,
)
