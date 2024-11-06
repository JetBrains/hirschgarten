package org.jetbrains.bazel.assets

object BazelPluginTemplates {
  val defaultBazelProjectViewContent: String =
    BazelPluginTemplates::class.java
      .getResource("/templates/defaultprojectview.bazelproject")
      ?.readText() ?: ""
}
