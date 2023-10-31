package org.jetbrains.bazel.config

import com.intellij.openapi.externalSystem.model.ProjectSystemId

internal object BazelPluginConstants {
  const val ID = "bazelbsp"
  val SYSTEM_ID = ProjectSystemId(ID, "Bazel BSP")
  val WORKSPACE_FILE_NAMES = listOf("WORKSPACE", "WORKSPACE.bazel", "MODULE.bazel", "WORKSPACE.bzlmod")
  val BUILD_FILE_NAMES = listOf("BUILD")
  val SUPPORTED_EXTENSIONS = listOf("bazel", "bazelproject", "bzlmod", "bzl")
  val SUPPORTED_CONFIG_FILE_NAMES = WORKSPACE_FILE_NAMES + BUILD_FILE_NAMES
}
