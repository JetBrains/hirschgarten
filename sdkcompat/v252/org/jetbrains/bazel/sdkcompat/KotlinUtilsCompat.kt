package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.modification.publishGlobalModuleStateModificationEvent

fun Project.publishGlobalModuleStateModificationEventCompat() = publishGlobalModuleStateModificationEvent()
