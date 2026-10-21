package org.jetbrains.bazel.data

import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.path.GlobalPaths
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.ide.starter.project.TestCaseTemplate
import org.jetbrains.bazel.test.compat.IntegrationTestCompat
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.kodein.di.instance
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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

object DevKitBazelCases : BaseBazelCasesParametrized(BazelTestContext.IDEA_DEVKIT_PLUGIN)

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
  private val description: String

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
    description = "Bundled Bazel test fixture '$path'"
  }

  override val isReusable: Boolean = false

  override val downloadTimeout: Duration = 1.minutes

  /**
   * Copies the fixture to a new dir that only the calling context uses.
   * The dir name keeps the fixture name. Another test of the same fixture, or an earlier run, cannot leave files in it.
   */
  @OptIn(ExperimentalPathApi::class)
  override fun downloadAndUnpackProject(): Path? {
    if (fixtureRoot.notExists()) return null
    val globalPaths by di.instance<GlobalPaths>()
    val copiesRoot = globalPaths.cacheDirForProjects.resolve("unpacked").createDirectories()
    val copyRoot = Files.createTempDirectory(copiesRoot, "p")
    val copiedFixtureRoot = fixtureRoot.copyToRecursively(copyRoot.resolve(fixtureRoot.name), followLinks = false, overwrite = false)
      .toAbsolutePath()
    val innerPath = pathWithinFixture ?: return copiedFixtureRoot
    val projectHome = copiedFixtureRoot.resolve(innerPath).normalize()
    require(projectHome.startsWith(copiedFixtureRoot) && projectHome.exists()) {
      "Bazel test fixture path does not exist: '$innerPath' in '$fixtureRoot'"
    }
    return projectHome
  }

  override fun getDescription(): String = description

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
