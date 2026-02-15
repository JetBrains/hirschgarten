package org.jetbrains.bazel.tests.settings

import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.ide.starter.driver.engine.BackgroundRun

fun projectViewOpen(bgRun: BackgroundRun) {
  bgRun.driver.withContext {
    ideFrame {
      shouldBe("README.md should be opened") {
        editorTabs().isTabOpened("README.md")
      }
      shouldBe("projectview.bazelproject should be opened") {
        editorTabs().isTabOpened("projectview.bazelproject")
      }
    }
  }
}
