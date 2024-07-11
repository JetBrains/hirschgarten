package org.jetbrains.bazel.assets

internal object BspPluginTemplates {
  val defaultBazelProjectViewContent = BspPluginTemplates::class.java
    .getResource("/templates/defaultprojectview.bazelproject")
    ?.readText() ?: ""
}
