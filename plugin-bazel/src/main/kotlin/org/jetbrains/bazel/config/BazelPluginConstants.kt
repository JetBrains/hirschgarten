package org.jetbrains.bazel.config

import com.intellij.openapi.externalSystem.model.ProjectSystemId

object BazelPluginConstants {
  const val BAZEL_TOOLWINDOW_ID: String = "Bazel"

  /**
   * This id used in [BazelMoveDeclarationsToFileRefactoringListener]. Update the usage there if changing the constant
   */
  const val ID = "bazel"
  const val BAZEL_DISPLAY_NAME = BAZEL_TOOLWINDOW_ID
  val SYSTEM_ID = ProjectSystemId(ID, BAZEL_DISPLAY_NAME)
  const val SE_LABEL_PROVIDER_ID: String = "LabelSearchEverywhereContributor"
}
