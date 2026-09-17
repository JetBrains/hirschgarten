package org.jetbrains.bazel.tests.sync

import com.intellij.driver.client.service
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.actionHyperlinkLabel
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.driver.sdk.ui.should
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.failIfFatalIdeErrorsPresent
import org.jetbrains.bazel.base.findFile
import org.jetbrains.bazel.base.openFile
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.waitForSyncSucceeded
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.tests.combined.VirtualFileManager
import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

private val RESYNC_ACTIONS_PROJECT = simpleBazelProject(path = "simpleJavaTest")

private const val ADDED_SOURCE_FILE = "resyncBanner/ResyncBannerTest.java"
private const val ADDED_BUILD_FILE = "resyncBanner/BUILD"

private val ADDED_SOURCE_FILE_CONTENT = """
  package org.example.resyncBanner;

  import org.junit.Test;

  import static org.junit.Assert.assertEquals;

  public class ResyncBannerTest {
      @Test
      public void basic() throws Exception {
          assertEquals(2, 1 + 1);
      }
  }
""".trimIndent() + "\n"

private val ADDED_BUILD_FILE_CONTENT = """
  java_test(
      name = "resyncBanner",
      srcs = ["ResyncBannerTest.java"],
      test_class = "org.example.resyncBanner.ResyncBannerTest",
      deps = [
          "@maven//:junit_junit",
      ],
  )
""".trimIndent()

private val RESYNC_DIRECTORY_ITEM = BazelPluginBundle.message("resync.directories.action.text", 1)

private val RESYNC_FILE_ITEM = BazelPluginBundle.message("resync.files.action.text", 1)

private val RESYNC_FILE_BANNER_LINK = BazelPluginBundle.message("sync.status.unsynced.source.file.banner.resync.file")

private fun partialResyncConsoleText(targetCount: Int): String =
  BazelBackendBundle.message("progress.text.resyncing.targets", targetCount)

private fun IdeaFrameUI.waitForPartialResync(phase: String, targetCount: Int) {
  val expected = partialResyncConsoleText(targetCount)
  x { byType("com.intellij.build.BuildView") }
    .should("The sync console reports '$expected' after $phase", 5.minutes) {
      getAllTexts().any { it.text.contains(expected) }
    }
}

private fun IdeaFrameUI.addProjectFile(context: IDETestContext, relativePath: String, content: String) {
  val file = context.resolvedProjectHome / relativePath
  file.parent.createDirectories()
  file.writeText(content)
  driver.service<VirtualFileManager>().asyncRefresh()
  waitForIndicators(5.minutes)
  waitFor(timeout = 2.minutes) { driver.findFile(relativePath) != null }
}

/**
 * ```sh
 * bazel test //plugins/bazel/integrationTests:integrationTests_test --test_env=JB_TEST_FILTER=org.jetbrains.bazel.tests.sync.ResyncActionsTest --test_output=errors --nocache_test_results
 * ```
 */
class ResyncActionsTest : IdeStarterBaseProjectTest() {
  @Test
  fun `resync action names the selection and syncs it`() {
    val context = createContext("resyncActions", IdeaBazelCases.withProject(RESYNC_ACTIONS_PROJECT))
    context.runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
      ideFrame {
        syncBazelProject()
        waitForIndicators(5.minutes)
        waitForSyncSucceeded()

        step("resync directory") {
          projectView().projectViewTree.rightClickRow { it.contains("basenameConflict") }
          popupMenu().waitFound()
          takeScreenshot("resyncDirectoryPopup")
          popupMenu().should("The project view popup offers '$RESYNC_DIRECTORY_ITEM'", 1.minutes) {
            itemsList().any { it == RESYNC_DIRECTORY_ITEM }
          }
          keyboard { escape() }
        }

        step("resync BUILD file") {
          projectView().projectViewTree.rightClickRow { it == "BUILD" }
          popupMenu().waitFound()
          takeScreenshot("resyncBuildFilePopup")
          popupMenu().select(RESYNC_FILE_ITEM)
          waitForIndicators(5.minutes)
          takeScreenshot("afterResyncBuildFile")
          waitForPartialResync(RESYNC_FILE_ITEM, targetCount = 1)
          driver.failIfFatalIdeErrorsPresent(RESYNC_FILE_ITEM)
        }

        step("resync file using banner action") {
          addProjectFile(context, ADDED_SOURCE_FILE, ADDED_SOURCE_FILE_CONTENT)
          driver.openFile(ADDED_SOURCE_FILE, waitForCodeAnalysis = false)
          val resyncFileLink = actionHyperlinkLabel(RESYNC_FILE_BANNER_LINK)
          resyncFileLink.waitFound(2.minutes)
          takeScreenshot("unsyncedFileBanner")

          addProjectFile(context, ADDED_BUILD_FILE, ADDED_BUILD_FILE_CONTENT)

          resyncFileLink.click()
          waitForIndicators(5.minutes)
          takeScreenshot("afterResyncFile")
          waitFor(timeout = 2.minutes) { resyncFileLink.notPresent() }
          driver.failIfFatalIdeErrorsPresent(RESYNC_FILE_BANNER_LINK)
        }
      }
    }
  }
}
