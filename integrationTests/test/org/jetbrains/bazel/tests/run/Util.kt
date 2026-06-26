package org.jetbrains.bazel.tests.run

import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.popups.runConfigurationsList
import com.intellij.driver.sdk.ui.components.common.popups.runConfigurationsPopup
import com.intellij.driver.sdk.withRetries

// Clicking the run config widget sometimes focuses it without opening the dropdown,
// especially after focus was elsewhere (editor, build tool window). Retry the click
// until the popup actually appears. ~9% flake rate observed without retries.
fun IdeaFrameUI.selectRunConfiguration(targetText: String) {
  withRetries(message = "Select run configuration '$targetText'", times = 3) {
    runConfigurationsPopup {
      runConfigurationsList {
        clickItem(targetText, fullMatch = false)
      }
    }
  }
}
