package org.jetbrains.bazel.config

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.buildToolId

val Project.isBazelProject: Boolean
  get() = buildToolId == BazelPluginConstants.bazelBspBuildToolId
