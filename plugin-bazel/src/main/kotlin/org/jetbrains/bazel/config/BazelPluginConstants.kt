package org.jetbrains.bazel.config

import com.intellij.openapi.externalSystem.model.ProjectSystemId

object BazelPluginConstants {
  const val ID = "bazel"
  const val BAZEL_DISPLAY_NAME = "Bazel"
  val SYSTEM_ID = ProjectSystemId(ID, BAZEL_DISPLAY_NAME)
}
