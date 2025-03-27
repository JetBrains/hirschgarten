package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.project.Project

val Project.isSharedSourceSupportEnabled: Boolean
  get() = false
