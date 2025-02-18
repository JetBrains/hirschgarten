package org.jetbrains.bazel.config

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.buildToolIdOrNull

val Project.isBazelProject: Boolean
  get() = buildToolIdOrNull == BazelPluginConstants.bazelBspBuildToolId
