package org.jetbrains.bazel.sdkcompat

import com.intellij.coverage.CoverageOptionsProvider
import com.intellij.openapi.project.Project

fun Project.shouldShowCoverageInProjectView(): Boolean = CoverageOptionsProvider.getInstance(this).showInProjectView()
