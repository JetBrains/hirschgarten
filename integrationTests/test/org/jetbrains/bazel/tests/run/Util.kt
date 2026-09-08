package org.jetbrains.bazel.tests.run

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.dialogs.EditRunConfigurationsDialogUiComponent
import com.intellij.driver.sdk.ui.components.common.popups.runConfigurationsList
import com.intellij.driver.sdk.ui.components.common.popups.runConfigurationsPopup
import com.intellij.driver.sdk.ui.components.elements.JTextComponentUI
import com.intellij.driver.sdk.ui.components.elements.textField
import com.intellij.driver.sdk.withRetries
import kotlin.time.Duration.Companion.seconds

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

fun Driver.closeCoverageReport() {
  invokeAction("HideCoverage")
}

var EditRunConfigurationsDialogUiComponent.bazelFlags: String
  get() = bazelFlagsField.text
  set(value) {
    bazelFlagsField.text = value
  }

private val EditRunConfigurationsDialogUiComponent.bazelFlagsField: JTextComponentUI
  get() = textField { byClass("ExpandableTextField").and(byAccessibleName("Bazel flags")) }
    .waitFound(10.seconds)
