package org.jetbrains.plugins.bsp.config

internal object BspPluginTemplates {
  val defaultBazelProjectViewContent = BspPluginTemplates::class.java
    .getResource("/templates/defaultprojectview.bazelproject")
    ?.readText() ?: ""
}
