package org.jetbrains.bazel.config

import com.intellij.openapi.externalSystem.model.ProjectSystemId

object BazelPluginConstants {
  const val ID = "bazel"
  const val BAZEL_DISPLAY_NAME = "Bazel"
  val SYSTEM_ID = ProjectSystemId(ID, BAZEL_DISPLAY_NAME)
  const val PROJECT_VIEW_FILE_EXTENSION = "bazelproject"
  const val MODULE_BAZEL_FILE_NAME = "MODULE.bazel"
  val WORKSPACE_FILE_NAMES = listOf(MODULE_BAZEL_FILE_NAME, "WORKSPACE", "WORKSPACE.bazel", "WORKSPACE.bzlmod")
  val BUILD_FILE_NAMES = listOf("BUILD.bazel", "BUILD")
  val SUPPORTED_EXTENSIONS = listOf("bazel", "bazelproject", "bzlmod", "bzl")
  val SUPPORTED_CONFIG_FILE_NAMES = WORKSPACE_FILE_NAMES + BUILD_FILE_NAMES

  fun defaultBuildFileName() = BUILD_FILE_NAMES.first()
}
