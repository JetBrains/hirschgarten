package org.jetbrains.bazel.config

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.buildToolIdOrNull

public val Project.isBazelProject: Boolean
  get() = buildToolIdOrNull == BazelPluginConstants.bazelBspBuildToolId
