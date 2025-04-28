package org.jetbrains.bazel.commons.constants

object Constants {
  const val NAME: String = "bazelbsp"
  const val VERSION: String = "3.2.0"
  const val CPP: String = "cpp"
  const val BAZEL_BUILD_COMMAND: String = "build"
  const val BAZEL_TEST_COMMAND: String = "test"
  const val BAZEL_COVERAGE_COMMAND: String = "coverage"
  const val ASPECT_REPOSITORY: String = "bazelbsp_aspect"
  const val ASPECTS_ROOT: String = "aspects"
  const val CORE_BZL: String = "core.bzl"
  const val EXTENSIONS_BZL: String = "extensions.bzl"
  const val TEMPLATE_EXTENSION: String = ".template"
  const val DOT_BAZELBSP_DIR_NAME: String = ".bazelbsp"

  const val WORKSPACE_FILE_NAME: String = "WORKSPACE"
  const val MODULE_BAZEL_FILE_NAME = "MODULE.bazel"
  val WORKSPACE_FILE_NAMES = listOf(MODULE_BAZEL_FILE_NAME, WORKSPACE_FILE_NAME, "WORKSPACE.bazel", "WORKSPACE.bzlmod")
  val BUILD_FILE_NAMES = listOf("BUILD.bazel", "BUILD")
  val SUPPORTED_EXTENSIONS = listOf("bazel", "bazelproject", "bzlmod", "bzl")
  val SUPPORTED_CONFIG_FILE_NAMES = WORKSPACE_FILE_NAMES + BUILD_FILE_NAMES

  fun defaultBuildFileName() = BUILD_FILE_NAMES.first()

  const val PROJECT_VIEW_FILE_EXTENSION = "bazelproject"
  const val DEFAULT_PROJECT_VIEW_FILE_NAME = ".bazelproject"
  const val LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME = "projectview.bazelproject"
}
