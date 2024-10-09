package org.jetbrains.bazel.config

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import org.jetbrains.plugins.bsp.config.BuildToolId

internal object BazelPluginConstants {
  val bazelBspBuildToolId = BuildToolId("bazelbsp")

  const val ID = "bazelbsp"
  val SYSTEM_ID = ProjectSystemId(ID, "Bazel")
  const val PROJECT_VIEW_FILE_EXTENSION = "bazelproject"
  val WORKSPACE_FILE_NAMES = listOf("WORKSPACE", "WORKSPACE.bazel", "MODULE.bazel", "WORKSPACE.bzlmod")
  val BUILD_FILE_NAMES = listOf("BUILD")
  val SUPPORTED_EXTENSIONS = listOf("bazel", "bazelproject", "bzlmod", "bzl")
  val SUPPORTED_CONFIG_FILE_NAMES = WORKSPACE_FILE_NAMES + BUILD_FILE_NAMES
}
