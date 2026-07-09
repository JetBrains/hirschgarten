package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.waitForSyncSucceeded
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.absolutePathString
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

@EnabledOnOs(OS.MAC)
internal class EnvironmentVariablesTest : IdeStarterBaseProjectTest() {
  @Test
  fun `sync succeeds if PATH variable was overridden`(@TempDir tempDir: Path) {
    val bazelWrapper = tempDir.resolve("bazel")
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

    createContext("simpleJavaCombined", IdeaBazelCases.SimpleJavaCombined)
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
