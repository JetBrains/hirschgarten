package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertFileInProject
import org.jetbrains.bazel.ideStarter.refreshFile
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.assertSyncedTargets
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.switchProjectView
import org.jetbrains.bazel.ideStarter.syncBazelProject
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
        syncBazelProject()
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
  fun `index should update when switching from all targets to subset targets`() = runTest(
    "Switching from all-targets to subset-targets view: file index should update, non-target files should remain visible",
  ) {
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
          execute { assertFileInProject("app/src/main/java/com/example/app/App.java", true) }
          execute { assertFileInProject("server/src/main/java/com/example/server/Server.java", true) }
          execute { assertFileInProject("frontend/src/main/java/com/example/frontend/Frontend.java", true) }
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
          execute { assertFileInProject("server/src/main/java/com/example/server/Server.java", true) }
          execute { assertFileInProject("frontend/src/main/java/com/example/frontend/Frontend.java", true) }
        }
      }
    }
  }

  // Verifies that modifying the active project view file directly (via Path.writeText)
  // and resyncing picks up the new target list — the basic project view edit workflow.
  // Switches back to projectview.bazelproject first, since @Order(1) may have changed
  // the active view to a different file.
  @Test @Order(2)
  fun `modifying active project view file and resyncing should update targets`() = runTest(
    "Modifying projectview.bazelproject on disk and resyncing: new targets should appear after resync",
  ) {
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
  fun `non-target directories should stay in project content when included via directories section`() = runTest(
    "directories: . with subset targets: files in non-target directories should remain in project content",
  ) {
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
        step("Verify files with matching targets are in project (sanity check)") {
          execute { assertFileInProject("app/src/main/java/com/example/app/App.java", true) }
          execute { assertFileInProject("common/src/main/java/com/example/common/Common.java", true) }
        }
        step("Verify files in non-target directories are still in project") {
          execute { assertFileInProject("server/src/main/java/com/example/server/Server.java", true) }
          execute { assertFileInProject("client/src/main/java/com/example/client/Client.java", true) }
          execute { assertFileInProject("database/src/main/java/com/example/database/Database.java", true) }
          execute { assertFileInProject("frontend/src/main/java/com/example/frontend/Frontend.java", true) }
          execute { assertFileInProject("webapp/src/main/java/com/example/webapp/Webapp.java", true) }
          execute { assertFileInProject("tools/src/main/java/com/example/tools/Tools.java", true) }
          execute { assertFileInProject("infra/src/main/java/com/example/infra/Infra.java", true) }
          execute { assertFileInProject("testing/src/main/java/com/example/testing/Testing.java", true) }
        }
      }
    }
  }

  // BAZEL-1986: directories: . -frontend -webapp explicitly excludes frontend/ and webapp/,
  // but targets: includes //frontend:frontend and //webapp:webapp.
  // Directory exclusions must take precedence — excluded files should NOT be in project content.
  @Test @Order(101)
  fun `excluded directories should not be scanned even when targets reference them`() = runTest(
    "BAZEL-1986: directories: . -frontend -webapp with targets referencing excluded dirs — excluded files should NOT be in project content",
  ) {
    withDriver(bgRun) {
      ideFrame {
        step("Switch to excluded-dir-with-target view") {
          execute { switchProjectView("excluded-dir-with-target.bazelproject") }
        }
        step("Resync with excluded dirs") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
        }
        step("Verify targets including excluded-dir targets are synced") {
          execute {
            assertSyncedTargets(
              "//app:app", "//common:common", "//server:server",
              "//frontend:frontend", "//webapp:webapp",
            )
          }
        }
        step("Verify excluded dir files are NOT in project content") {
          execute { assertFileInProject("frontend/src/main/java/com/example/frontend/Frontend.java", false) }
          execute { assertFileInProject("webapp/src/main/java/com/example/webapp/Webapp.java", false) }
        }
        step("Verify included dir files with targets ARE in project content (sanity check)") {
          execute { assertFileInProject("app/src/main/java/com/example/app/App.java", true) }
          execute { assertFileInProject("common/src/main/java/com/example/common/Common.java", true) }
          execute { assertFileInProject("server/src/main/java/com/example/server/Server.java", true) }
        }
        step("Verify included dir files without targets ARE in project content") {
          execute { assertFileInProject("client/src/main/java/com/example/client/Client.java", true) }
          execute { assertFileInProject("database/src/main/java/com/example/database/Database.java", true) }
          execute { assertFileInProject("tools/src/main/java/com/example/tools/Tools.java", true) }
          execute { assertFileInProject("infra/src/main/java/com/example/infra/Infra.java", true) }
          execute { assertFileInProject("testing/src/main/java/com/example/testing/Testing.java", true) }
        }
      }
    }
  }

  // BAZEL-2937: build_flags with --extra_toolchains causes bazel query to fail,
  // which breaks IndexAdditionalFilesSyncHook and makes all directories visible.
  // After sync with build_flags, directory scoping must still work correctly.
  @Test @Order(102)
  fun `build_flags should not break directory scoping`() = runTest(
    "BAZEL-2937: build_flags with --extra_toolchains should not break directory scoping via bazel query failure",
  ) {
    withDriver(bgRun) {
      ideFrame {
        step("Switch to build-flags-with-toolchain view") {
          execute { switchProjectView("build-flags-with-toolchain.bazelproject") }
        }
        step("Resync with build_flags") {
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          assertSyncSucceeded()
        }
        step("Verify included dir files ARE in project content") {
          execute { assertFileInProject("app/src/main/java/com/example/app/App.java", true) }
          execute { assertFileInProject("common/src/main/java/com/example/common/Common.java", true) }
        }
        step("Verify excluded dir files are NOT in project content") {
          execute { assertFileInProject("frontend/src/main/java/com/example/frontend/Frontend.java", false) }
          execute { assertFileInProject("webapp/src/main/java/com/example/webapp/Webapp.java", false) }
        }
        step("Verify dirs not in directories: section are NOT in project content") {
          execute { assertFileInProject("server/src/main/java/com/example/server/Server.java", false) }
          execute { assertFileInProject("database/src/main/java/com/example/database/Database.java", false) }
        }
      }
    }
  }

  // BAZEL-1451: derive_targets_from_directories: false with no targets: section.
  // The plugin should NOT silently fall back to //... (all targets).
  // Expected: zero targets synced (or an error), not a full-repo sync.
  @Test @Order(103)
  fun `empty targets with derive_targets_from_directories false should not fallback to all targets`() = runTest(
    "Empty targets with derive_targets_from_directories: false — should sync zero targets, not fall back to //...",
  ) {
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

  private fun runTest(description: String, block: () -> Unit) {
    try {
      block()
    } catch (e: Exception) {
      throw AssertionError(description, e)
    }
  }

  @AfterAll
  fun closeIde() {
    if (::bgRun.isInitialized) bgRun.closeIdeAndWait()
    if (::ctx.isInitialized) checkIdeaLogForExceptions(ctx)
  }
}
