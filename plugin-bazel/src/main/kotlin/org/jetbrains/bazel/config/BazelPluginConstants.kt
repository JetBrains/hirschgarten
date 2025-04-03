package org.jetbrains.bazel.config

import com.intellij.openapi.externalSystem.model.ProjectSystemId

object BazelPluginConstants {
  const val ID = "bazel"
  const val BAZEL_DISPLAY_NAME = "Bazel"
  val SYSTEM_ID = ProjectSystemId(ID, BAZEL_DISPLAY_NAME)
  const val PROJECT_VIEW_FILE_EXTENSION = "bazelproject"
  val WORKSPACE_FILE_NAMES = listOf("WORKSPACE", "WORKSPACE.bazel", "MODULE.bazel", "WORKSPACE.bzlmod")
  val BUILD_FILE_NAMES = listOf("BUILD", "BUILD.bazel")
  val SUPPORTED_EXTENSIONS = listOf("bazel", "bazelproject", "bzlmod", "bzl")
  val SUPPORTED_CONFIG_FILE_NAMES = WORKSPACE_FILE_NAMES + BUILD_FILE_NAMES
}
