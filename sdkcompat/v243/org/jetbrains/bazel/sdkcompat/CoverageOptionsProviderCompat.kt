package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.project.Project

@Suppress("UnusedReceiverParameter")
fun Project.shouldShowCoverageInProjectView(): Boolean = true
