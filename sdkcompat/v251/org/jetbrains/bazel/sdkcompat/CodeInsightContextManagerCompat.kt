package org.jetbrains.bazel.sdkcompat

import com.intellij.codeInsight.multiverse.CodeInsightContextManager
import com.intellij.openapi.project.Project

val Project.isSharedSourceSupportEnabled: Boolean
  get() = CodeInsightContextManager.getInstance(this).isSharedSourceSupportEnabled
