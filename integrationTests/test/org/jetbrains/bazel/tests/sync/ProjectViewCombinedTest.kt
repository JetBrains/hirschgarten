package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertFileKind
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.assertSyncedTargets
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.refreshFile
import org.jetbrains.bazel.ideStarter.switchProjectView
import org.jetbrains.bazel.ideStarter.switchProjectViewWithPreview
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.performanceImpl.FileKindCheck.INDEXABLE
import org.jetbrains.bazel.performanceImpl.FileKindCheck.IN_CONTENT
import org.jetbrains.bazel.performanceImpl.FileKindCheck.NON_INDEXABLE
import org.jetbrains.bazel.performanceImpl.FileKindCheck.OUTSIDE_CONTENT
import org.jetbrains.bazel.tests.ui.clickRunGutterOnLine
import org.jetbrains.bazel.tests.ui.getRunGutterOnLine
import org.jetbrains.bazel.tests.ui.verifyAvailableRunGutterActions
import org.jetbrains.bazel.tests.ui.verifyTestStatus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ProjectViewCombinedTest : IdeStarterBaseProjectTest() {
  private lateinit var bgRun: BackgroundRun
  private lateinit var ctx: IDETestContext

  @BeforeAll
  fun startIdeAndSync() {
    ctx = createContext("projectViewCombined", IdeaBazelCases.ProjectViewCombined)
    bgRun = ctx.runIdeWithDriver(runTimeout = timeout)
    withDriver(bgRun) {
      ideFrame {
        syncBazelProject(buildAndSync = true)
        waitForIndicators(5.minutes)
        assertSyncSucceeded()
      }
    }
  }

  @BeforeEach
  fun skipIfCriticalFailed() = Assumptions.assumeFalse(criticalProblemOccurred)

  @AfterEach
  fun checkIdeState() {
    if (!criticalProblemOccurred && ::bgRun.isInitialized && !bgRun.driver.isConnected) {
      criticalProblemOccurred = true
    }
  }

  // Verifies that switching from all-targets to subset-targets properly updates the file index
  // and non-target files remain visible (not stale from the previous broader sync)
  @Test @Order(1)
  fun `index should update when switching from all targets to subset targets`() {
    withDriver(bgRun) {
      ideFrame {
        step("Switch to all-dirs-all-targets view") {
          execute { switchProjectView("all-dirs-all-targets.bazelproject") }
        }
        step("Resync with all targets") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
        }
        step("Verify all files are in project with all targets (sanity check)") {
          execute { assertFileKind("app/src/main/java/com/example/app/App.java", IN_CONTENT) }
          execute { assertFileKind("server/src/main/java/com/example/server/Server.java", IN_CONTENT) }
          execute { assertFileKind("frontend/src/main/java/com/example/frontend/Frontend.java", IN_CONTENT) }
        }

        step("Switch back to subset targets view") {
          execute { switchProjectView("all-dirs-subset-targets.bazelproject") }
        }
        step("Resync with subset targets") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
        }
        step("Verify only subset targets are synced") {
          execute { assertSyncedTargets("//app:app", "//common:common") }
        }
        step("Verify non-target files are still in project after narrowing targets") {
          execute { assertFileKind("server/src/main/java/com/example/server/Server.java", IN_CONTENT) }
          execute { assertFileKind("frontend/src/main/java/com/example/frontend/Frontend.java", IN_CONTENT) }
        }
      }
    }
  }

  // Verifies that modifying the active project view file directly (via Path.writeText)
  // and resyncing picks up the new target list — the basic project view edit workflow.
  // Switches back to projectview.bazelproject first, since @Order(1) may have changed
  // the active view to a different file.
  @Test @Order(2)
  fun `modifying active project view file and resyncing should update targets`() {
    withDriver(bgRun) {
      ideFrame {
        step("Ensure projectview.bazelproject is the active view") {
          execute { switchProjectView("projectview.bazelproject") }
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
        }
        step("Verify initial targets from default project view") {
          execute { assertSyncedTargets("//app:app", "//common:common") }
        }
        step("Modify active project view — add server target") {
          openFile("projectview.bazelproject", waitForCodeAnalysis = false)
          (ctx.resolvedProjectHome / "projectview.bazelproject")
            .writeText(
              "derive_targets_from_directories: false\n" +
                "index_all_files_in_directories: true\n\n" +
                "directories:\n  .\n\n" +
                "targets:\n  //app:app\n  //common:common\n  //server:server\n\n" +
                "import_depth: 0\n",
            )
          execute { refreshFile("projectview.bazelproject") }
          wait(3.seconds)
        }
        step("Resync and verify server target is now synced") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
          execute { assertSyncedTargets("//app:app", "//common:common", "//server:server") }
        }
        step("Restore default project view and resync to clean state") {
          (ctx.resolvedProjectHome / "projectview.bazelproject")
            .writeText(
              "derive_targets_from_directories: false\n" +
                "index_all_files_in_directories: true\n\n" +
                "directories:\n  .\n\n" +
                "targets:\n  //app:app\n  //common:common\n\n" +
                "import_depth: 0\n",
            )
          execute { refreshFile("projectview.bazelproject") }
          wait(3.seconds)
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
          execute { assertSyncedTargets("//app:app", "//common:common") }
        }
      }
    }
  }

  // BAZEL-2082: directories: . includes all dirs, but only 2 out of 10 have targets.
  // Files in the other 8 directories (including custom_java_lib rule targets) must remain in project content.
  @Test @Order(100)
  fun `non-target directories should stay in project content when included via directories section`() {
    withDriver(bgRun) {
      ideFrame {
        step("Switch to all-dirs-subset-targets view") {
          execute { switchProjectView("all-dirs-subset-targets.bazelproject") }
        }
        step("Resync with subset targets") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
        }
        step("Verify only subset targets are synced") {
          execute { assertSyncedTargets("//app:app", "//common:common") }
        }
        step("Verify files with matching targets are in project and indexable (sanity check)") {
          execute { assertFileKind("app/src/main/java/com/example/app/App.java", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("common/src/main/java/com/example/common/Common.java", IN_CONTENT, INDEXABLE) }
        }
        step("Verify files in non-target directories are still in project and indexable") {
          execute { assertFileKind("server/src/main/java/com/example/server/Server.java", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("client/src/main/java/com/example/client/Client.java", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("database/src/main/java/com/example/database/Database.java", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("frontend/src/main/java/com/example/frontend/Frontend.java", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("webapp/src/main/java/com/example/webapp/Webapp.java", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("tools/src/main/java/com/example/tools/Tools.java", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("infra/src/main/java/com/example/infra/Infra.java", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("testing/src/main/java/com/example/testing/Testing.java", IN_CONTENT, INDEXABLE) }
        }
      }
    }
  }

  // BAZEL-1986: project view with only targets (no directories section).
  // All BUILD files should be indexed, so smart code features work in them properly.
  @Test @Order(101)
  fun `non-targeted BUILD files should also be indexed when using the targets-only project view`() {
    withDriver(bgRun) {
      ideFrame {
        step("Switch to targets-but-not-dirs view") {
          execute { switchProjectView("targets-but-not-dirs.bazelproject") }
        }
        step("Resync with targets-only view") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
        }
        step("Verify synced targets") {
          execute { assertSyncedTargets("//app:app", "//common:common", "//server:server") }
        }
        step("Verify non-targeted BUILD files ARE indexable") {
          execute { assertFileKind("frontend/BUILD.bazel", INDEXABLE) }
          execute { assertFileKind("webapp/BUILD.bazel", INDEXABLE) }
        }
        step("Verify non-targeted source files are NOT indexable") {
          execute { assertFileKind("frontend/src/main/java/com/example/frontend/Frontend.java", NON_INDEXABLE) }
          execute { assertFileKind("webapp/src/main/java/com/example/webapp/Webapp.java", NON_INDEXABLE) }
        }
        step("Verify targeted files ARE indexable") {
          execute { assertFileKind("app/src/main/java/com/example/app/App.java", INDEXABLE) }
          execute { assertFileKind("common/src/main/java/com/example/common/Common.java", INDEXABLE) }
          execute { assertFileKind("server/src/main/java/com/example/server/Server.java", INDEXABLE) }
        }
      }
    }
  }

  // BAZEL-2937: build_flags with --extra_toolchains causes bazel query to fail,
  // which breaks IndexAdditionalFilesSyncHook and makes all directories visible.
  // After sync with build_flags, directory scoping must still work correctly.
  @Test @Order(102)
  fun `build_flags should not break directory scoping`() {
    withDriver(bgRun) {
      ideFrame {
        step("Switch to build-flags-with-toolchain view") {
          switchProjectViewWithPreview("build-flags-with-toolchain.bazelproject")
        }
        step("Resync with build_flags") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
        }
        step("Verify included dir files ARE in project content and indexable") {
          execute { assertFileKind("app/src/main/java/com/example/app/App.java", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("common/src/main/java/com/example/common/Common.java", IN_CONTENT, INDEXABLE) }
        }
        step("Verify excluded dir files are NOT in project content") {
          execute { assertFileKind("frontend/src/main/java/com/example/frontend/Frontend.java", OUTSIDE_CONTENT) }
          execute { assertFileKind("webapp/src/main/java/com/example/webapp/Webapp.java", OUTSIDE_CONTENT) }
        }
        step("Verify dirs not in directories: section ARE in project content and NOT indexable") {
          execute { assertFileKind("server/src/main/java/com/example/server/Server.java", IN_CONTENT, NON_INDEXABLE) }
          execute { assertFileKind("database/src/main/java/com/example/database/Database.java", IN_CONTENT, NON_INDEXABLE) }
        }
      }
    }
  }

  // Verifies that Bazel config files like MODULE.bazel, WORKSPACE.bazel, etc. residing in the
  // root project directory are always indexed when the projectview file doesn't contain "." in the
  // "directories" section
  @Test @Order(103)
  fun `bazel configs in root project directory should be indexed`() {
    withDriver(bgRun) {
      ideFrame {
        step("Switch to subset-dirs-derive-targets view") {
          switchProjectViewWithPreview("subset-dirs-derive-targets.bazelproject")
        }
        step("Resync with subset-dirs-derive-targets view") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
        }
        step("Verify bazel configs in workspace root ARE in project content and indexable") {
          execute { assertFileKind("MODULE.bazel", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("subset-dirs-derive-targets.bazelproject", IN_CONTENT, INDEXABLE) }
        }
        step("Verify other files in workspace root ARE in project content and NOT indexable") {
          execute { assertFileKind(".bazelversion", IN_CONTENT, NON_INDEXABLE) }
          execute { assertFileKind("MODULE.bazel.lock", IN_CONTENT, NON_INDEXABLE) }
          execute { assertFileKind("all-dirs-all-targets.bazelproject", IN_CONTENT, NON_INDEXABLE) }
          execute { assertFileKind("build-flags-with-toolchain.bazelproject", IN_CONTENT, NON_INDEXABLE) }
          execute { assertFileKind("targets-but-not-dirs.bazelproject", IN_CONTENT, NON_INDEXABLE) }
        }
      }
    }
  }

  // BAZEL-1451: derive_targets_from_directories: false with no targets: section.
  // The plugin should NOT silently fall back to //... (all targets).
  // Expected: zero targets synced (or an error), not a full-repo sync.
  @Test @Order(104)
  fun `empty targets with derive_targets_from_directories false should not fallback to all targets`() {
    withDriver(bgRun) {
      ideFrame {
        step("Switch to no-targets-no-derive view") {
          execute { switchProjectView("no-targets-no-derive.bazelproject") }
        }
        step("Resync with empty targets") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
        }
        step("Verify no targets are synced (not a fallback to //...)") {
          execute { assertSyncedTargets() }
        }
      }
    }
  }

  @Test
  @Order(105)
  fun `targets outside the selected project view must have run gutters`() {
    withDriver(bgRun) {
      ideFrame {
        step("Switch to no-targets-no-derive view") {
          execute { switchProjectView("no-targets-no-derive.bazelproject") }
        }
        step("Resync") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
        }
        step("Open the BUILD file inside //binary") {
          openFile("binary/BUILD.bazel")
        }
        step("Check run gutters for java_binary") {
          clickRunGutterOnLine(4)
          verifyAvailableRunGutterActions(listOf("Build Target", "Run", "Debug run"))
        }
        step("Check run gutter for custom_java_lib") {
          val gutter = getRunGutterOnLine(10)
          check("build.svg" in gutter.getIconPath()) {
            "Should be one single Compile icon for custom_java_lib"
          }
          gutter.click()
        }
        val consoleView = x { byClass("ConsoleViewImpl") }
        step("Check run gutters for custom_binary") {
          clickRunGutterOnLine(15)
          verifyAvailableRunGutterActions(listOf("Build Target", "Run"))
          popup().waitOneText("Run").click()
          consoleView.waitContainsText("Hello, world!")
        }
        step("Scroll down") {
          execute { goto(32, 1) }
        }
        step("Check run gutters for java_test") {
          clickRunGutterOnLine(21)
          verifyAvailableRunGutterActions(listOf("Build Target", "Test", "Debug test", "Run with Coverage"))
        }
        step("Check run gutters for custom_test") {
          clickRunGutterOnLine(27)
          verifyAvailableRunGutterActions(listOf("Build Target", "Test", "Run with Coverage"))
          popup().waitOneText("Run with Coverage").click()
          verifyTestStatus(
            listOf("1 test passed"),
            listOf("com.example.MainTest", "testAdd"),
          )
        }
        step("Scroll up") {
          execute { goto(1, 1) }
        }
        step("Debug run java_binary") {
          clickRunGutterOnLine(4)
          popup().waitOneText("Debug run").click()
          consoleView.waitContainsText("Hello, world!")
        }
        step("Check there's no run gutters in MODULE.bazel") {
          openFile("MODULE.bazel")
          val gutterIcons =
            try {
              editorTabs()
                .gutter()
                .getGutterIcons()
            }
            catch (_: WaitForException) {
              emptyList()
            }
          check(gutterIcons.isEmpty()) { "Expected no gutter icons in MODULE.bazel, got: $gutterIcons" }
        }
      }
    }
  }

  @AfterAll
  fun closeIde() {
    if (::bgRun.isInitialized) bgRun.closeIdeAndWait()
    if (::ctx.isInitialized) checkIdeaLogForExceptions(ctx)
  }
}
