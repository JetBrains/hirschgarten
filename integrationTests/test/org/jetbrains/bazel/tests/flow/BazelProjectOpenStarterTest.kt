package org.jetbrains.bazel.tests.flow

import com.intellij.driver.client.Remote
import com.intellij.driver.client.service
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitFor
import com.intellij.driver.sdk.waitForProjectOpen
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertSyncedTargets
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.isBazelProject
import org.jetbrains.bazel.ideStarter.projectRootDir
import org.jetbrains.bazel.ideStarter.singleProjectOrNull
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.ideStarter.waitForSyncSucceeded
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.tests.ui.setAutoOpenProjectIfPresent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.minutes
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.time.Duration

// TODO: temporary pin to SBPFT branch bazel/dan/e2e-os-bazel-matrix; repoint to main once the fixture upstreaming lands there
private const val BAZEL_PROJECT_OPEN_REVISION = "e974ca77b97e65a329f03492f9b556e44f47f648"

private fun bazelProjectOpenProject(
  projectPath: String,
  configureProject: (com.intellij.ide.starter.ide.IDETestContext) -> Unit = BazelProjectConfigurer::configureProjectBeforeUse,
) = simpleBazelProject(
  revision = BAZEL_PROJECT_OPEN_REVISION,
  path = projectPath,
  configureProject = configureProject,
)

private val BAZEL_PROJECT_ROOT = bazelProjectOpenProject("simpleKotlinTest")
private val BAZEL_MODULE_FILE = bazelProjectOpenProject("simpleKotlinTest/MODULE.bazel")
private val LEGACY_BAZEL_PROJECT = bazelProjectOpenProject("legacyGooglePluginTest/.ijwb")
private val BAZEL_PROJECT_WITH_DOT_IDEA = bazelProjectOpenProject(
  projectPath = "simpleJavaTest",
  configureProject = { context ->
    BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(
      context,
      removeDotIdea = false,
    )
  },
)

class BazelProjectOpenStarterTest : IdeStarterBaseProjectTest() {

  @Test
  fun `open project by root directory should resolve project name`() {
    val context = createContext("openBazelProjectByProjectRoot", IdeaBazelCases.withProject(BAZEL_PROJECT_ROOT))
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure correct project is opened") {
          waitForProjectOpen()
          assertEquals("simpleKotlinTest", singleProject().getName())
          assertEquals("simpleKotlinTest", projectRootDir.getName())
        }
        step("Ensure project was opened as a Bazel project") {
          assertTrue(isBazelProject)
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `open project by MODULE file should resolve project name`() {
    val context = createContext("openBazelProjectByProjectModule", IdeaBazelCases.withProject(BAZEL_MODULE_FILE))
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure correct project is opened") {
          waitForProjectOpen()
          assertEquals("simpleKotlinTest", singleProject().getName())
          assertEquals("simpleKotlinTest", projectRootDir.getName())
        }
        step("Ensure project was opened as a Bazel project") {
          assertTrue(isBazelProject)
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `open legacy ijwb project should reopen with MODULE bazel`() {
    val context = createContext("openLegacyProject", IdeaBazelCases.withProject(LEGACY_BAZEL_PROJECT))
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure legacy project reopened with MODULE.bazel") {
          waitFor(timeout = 1.minutes, message = "Project not reopened with MODULE.bazel!") {
            singleProjectOrNull()?.getPresentableUrl()?.endsWith("MODULE.bazel") == true
          }
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `should not open directory as a bazel project when it contains a dot idea directory`() {
    val context = createContext("projectWithDotIdeaDir", IdeaBazelCases.withProject(BAZEL_PROJECT_WITH_DOT_IDEA))
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure correct project was opened") {
          waitForProjectOpen()
          assertEquals("simpleJavaTest", singleProject().getName())
        }
        step("Ensure that project is not a Bazel project") {
          assertFalse(isBazelProject)
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `should open directory as a bazel project when it contains a dot idea directory and registry flag is enabled`() {
    val context = createContext("projectWithDotIdeaDir", IdeaBazelCases.withProject(BAZEL_PROJECT_WITH_DOT_IDEA))
    context
      .setAutoOpenProjectIfPresent(true)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure correct project was opened") {
          waitForProjectOpen()
          assertEquals("simpleJavaTest", singleProject().getName())
          assertEquals("simpleJavaTest", projectRootDir.getName())
        }
        step("Ensure project was opened as a Bazel project") {
          assertTrue(isBazelProject)
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `opening a project with no project view file should not cause any issues`() {
    val context = createContext("openSimpleJavaTestWithProjectView", IdeaBazelCases.withProject(BAZEL_PROJECT_WITH_DOT_IDEA))
    context
      .setAutoOpenProjectIfPresent(true)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure project was opened as a Bazel project") {
          waitForProjectOpen()
          assertTrue(isBazelProject)
        }
        step("Ensure project view file was loaded") {
          val isProjectViewEmpty = service<RemoteProjectViewService>(singleProject())
            .getProjectView()
            .isEmpty()
          assertFalse(isProjectViewEmpty)
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `open project with an broken project view`() {
    val context = createContext("openProjectWithUnresolvedImport", IdeaBazelCases.withProject(BAZEL_PROJECT_ROOT))

    val projectViewFile = context.resolvedProjectHome / ".bazelproject"
    projectViewFile.writeText(
      "import does_not_exist.bazelproject\n" + // unresolved import
        "import .bazelproject\n" +             // recursive import
        projectViewFile.readText()
    )

    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Project opens as a Bazel project despite the unresolved required import") {
          waitForProjectOpen()
          assertTrue(isBazelProject)
        }
        ideFrame {
          syncBazelProject()
          step("Unresolved required import is reported as an error in the sync console") {
            waitForUnresolvedImportReported("does_not_exist.bazelproject")
          }
          waitForSyncSucceeded()
          step("Well-formed targets still sync") {
            execute {
              assertSyncedTargets(
                "//:B",
                "//:C",
                "//:SimpleKotlinTest",
                "//:associates_example",
                "//:requires-no-ide",
                "//:not-ignored",
              )
            }
          }
        }
      }
    checkIdeaLogForExceptions(context)
  }

  private fun UiComponent.waitForUnresolvedImportReported(importPath: String, timeout: Duration = 2.minutes) {
    val buildView = x { byType("com.intellij.build.BuildView") }
    val importErrorText = BazelPluginBundle.message("project.view.import.unresolved", importPath)
    buildView.waitAnyTexts(message = "Waiting for the unresolved import diagnostic", timeout = timeout) {
      it.text.contains(importErrorText)
    }
  }
}

@Remote("org.jetbrains.bazel.languages.projectview.ProjectViewService", plugin = "org.jetbrains.bazel/intellij.bazel.projectview")
interface RemoteProjectViewService {
  fun getProjectView(): RemoteProjectView
}

@Remote("org.jetbrains.bazel.languages.projectview.ProjectView", plugin = "org.jetbrains.bazel/intellij.bazel.projectview")
interface RemoteProjectView {
  fun isEmpty(): Boolean
}
