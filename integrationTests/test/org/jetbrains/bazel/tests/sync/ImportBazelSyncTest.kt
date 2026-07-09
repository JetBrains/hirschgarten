package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.checkIdeaLogForExceptions
import org.jetbrains.bazel.base.getProjectInfoFromSystemProperties
import org.jetbrains.bazel.base.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class ImportBazelSyncTest : IdeStarterBaseProjectTest() {

  @Test
  fun `large Bazel project should sync successfully`() {
    val context = createContext("bazel-sync", IdeaBazelCases.withProject(getProjectInfoFromSystemProperties()))
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(10.minutes)
        }
      }
    checkIdeaLogForExceptions(context)
  }
}
