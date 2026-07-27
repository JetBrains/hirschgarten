package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.data.IdeStarterOs
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.waitForSyncSucceeded
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.absolutePathString
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

private val ENVIRONMENT_VARIABLES_PROJECT = simpleBazelProject(
  revision = "5cbca8140ac85e5178ca803935fbd9e1d9400a07",
  path = "simpleJavaTest",
)

internal class EnvironmentVariablesTest : IdeStarterBaseProjectTest() {
  @Test
  fun `sync succeeds if PATH variable was overridden`(@TempDir tempDir: Path) {
    val isWindows = IdeStarterOs.current() == IdeStarterOs.WINDOWS
    val bazelWrapper = tempDir.resolve(if (isWindows) "bazel.bat" else "bazel")
    if (isWindows) {
      bazelWrapper.writeText("@echo off\necho This is not actually Bazel :)\nexit /b 1\n")
    } else {
      bazelWrapper.writeText(
        """
          #!/bin/sh
          echo "This is not actually Bazel :)"
          exit 1
        """.trimIndent(),
      )
      bazelWrapper.setPosixFilePermissions(
        setOf(
          PosixFilePermission.OWNER_READ,
          PosixFilePermission.GROUP_READ,
          PosixFilePermission.OTHERS_READ,
          PosixFilePermission.OWNER_EXECUTE,
          PosixFilePermission.GROUP_EXECUTE,
          PosixFilePermission.OTHERS_EXECUTE,
        ),
      )
    }

    createContext("simpleJavaCombined", IdeaBazelCases.withProject(ENVIRONMENT_VARIABLES_PROJECT))
      .applyVMOptionsPatch { withEnv("PATH", tempDir.absolutePathString() + File.pathSeparator + System.getenv("PATH")) }
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          step("Initial sync") {
            syncBazelProject()
            waitForIndicators(5.minutes)
            takeScreenshot("afterSync")
            waitForSyncSucceeded()
          }
        }
      }
  }
}
