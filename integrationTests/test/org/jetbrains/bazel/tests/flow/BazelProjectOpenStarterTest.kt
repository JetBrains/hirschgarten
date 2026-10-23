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
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.assertSyncedTargets
import org.jetbrains.bazel.base.checkIdeaLogForExceptions
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.isBazelProject
import org.jetbrains.bazel.base.projectRootDir
import org.jetbrains.bazel.base.singleProjectOrNull
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.waitForSyncSucceeded
import org.jetbrains.bazel.symlinks.SymlinkTestConditions
import org.jetbrains.bazel.tests.ui.setAutoOpenProjectIfPresent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.minutes
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.time.Duration

private fun bazelProjectOpenProject(
  projectPath: String,
  configureProject: (com.intellij.ide.starter.ide.IDETestContext) -> Unit = BazelProjectConfigurer::configureProjectBeforeUse,
  symlinkPath: String? = null,
) = simpleBazelProject(
  path = projectPath,
  configureProject = configureProject,
  symlinkPath = symlinkPath,
)

private val BAZEL_PROJECT_ROOT = bazelProjectOpenProject("simpleKotlinTest")
private val BAZEL_MODULE_FILE = bazelProjectOpenProject("simpleKotlinTest/MODULE.bazel")
private val BAZEL_PROJECT_ROOT_THROUGH_SYMLINK = bazelProjectOpenProject("simpleKotlinTest", symlinkPath = "simpleKotlinTestLink")
private val BAZEL_MODULE_FILE_THROUGH_SYMLINK = bazelProjectOpenProject("simpleKotlinTest/MODULE.bazel", symlinkPath = "simpleKotlinTestLink")
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
  fun `open project by symlinked root directory should use the real root directory`() {
    val context = createContext("openBazelProjectBySymlinkedRoot", IdeaBazelCases.withProject(BAZEL_PROJECT_ROOT_THROUGH_SYMLINK))
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure the project uses the real root directory") {
          waitForProjectOpen()
          assertEquals("simpleKotlinTest", singleProject().getName())
          assertEquals(context.resolvedProjectHome.toRealPath(), Path(projectRootDir.getPath()))
        }
        step("Ensure project was opened as a Bazel project") {
          assertTrue(isBazelProject)
        }
        ideFrame {
          syncBazelProject()
          waitForSyncSucceeded()
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `open project by MODULE file under symlinked root directory should use the real root directory`() {
    val context = createContext("openBazelProjectByModuleUnderSymlinkedRoot", IdeaBazelCases.withProject(BAZEL_MODULE_FILE_THROUGH_SYMLINK))
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure the project uses the real root directory") {
          waitForProjectOpen()
          assertEquals("simpleKotlinTest", singleProject().getName())
          assertEquals(context.resolvedProjectHome.parent.toRealPath(), Path(projectRootDir.getPath()))
        }
        step("Ensure project was opened as a Bazel project") {
          assertTrue(isBazelProject)
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  @EnabledIf(SymlinkTestConditions.IS_SYM_LINK_CREATION_SUPPORTED)
  fun `open project with symlinked MODULE file should use the directory of the symlink`() {
    val context = createContext("openBazelProjectWithSymlinkedModule", IdeaBazelCases.withProject(BAZEL_PROJECT_ROOT))
    val moduleFile = context.resolvedProjectHome / "MODULE.bazel"
    val sharedModuleFile = context.resolvedProjectHome.resolveSibling("shared").createDirectories() / "MODULE.bazel"
    moduleFile.moveTo(sharedModuleFile)
    moduleFile.createSymbolicLinkPointingTo(sharedModuleFile)

    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure the project uses the directory of the symlink") {
          waitForProjectOpen()
          assertEquals("simpleKotlinTest", singleProject().getName())
          assertEquals(context.resolvedProjectHome.toRealPath(), Path(projectRootDir.getPath()))
        }
        step("Ensure project was opened as a Bazel project") {
          assertTrue(isBazelProject)
        }
        ideFrame {
          syncBazelProject()
          waitForSyncSucceeded()
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
      "try_import does_not_exist.bazelproject\n" + // unresolved import
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
}

@Remote("org.jetbrains.bazel.languages.projectview.ProjectViewService", plugin = "org.jetbrains.bazel/intellij.bazel.projectview")
interface RemoteProjectViewService {
  fun getProjectView(): RemoteProjectView
}

@Remote("org.jetbrains.bazel.languages.projectview.ProjectView", plugin = "org.jetbrains.bazel/intellij.bazel.projectview")
interface RemoteProjectView {
  fun isEmpty(): Boolean
}
