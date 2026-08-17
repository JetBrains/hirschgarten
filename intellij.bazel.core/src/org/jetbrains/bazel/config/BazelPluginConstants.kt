package org.jetbrains.bazel.config

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants

@ApiStatus.Internal
object BazelPluginConstants {
  const val BAZEL_TOOLWINDOW_ID: String = Constants.BAZEL_DISPLAY_NAME

  /**
   * This id used in [BazelMoveDeclarationsToFileRefactoringListener]. Update the usage there if changing the constant
   */
  const val ID = "bazel"
  const val BAZEL_DISPLAY_NAME = BAZEL_TOOLWINDOW_ID
  val SYSTEM_ID = ProjectSystemId(ID, BAZEL_DISPLAY_NAME)
  const val SE_LABEL_PROVIDER_ID: String = "LabelSearchEverywhereContributor"
}
