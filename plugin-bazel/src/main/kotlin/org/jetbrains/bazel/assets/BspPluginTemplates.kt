package org.jetbrains.bazel.assets

object BspPluginTemplates {
  val defaultBazelProjectViewContent =
    BspPluginTemplates::class.java
      .getResource("/templates/defaultprojectview.bazelproject")
      ?.readText() ?: ""
}
