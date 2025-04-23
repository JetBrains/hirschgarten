package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics

fun Project.publishGlobalModuleStateModificationEventCompat() =
  analysisMessageBus
    .syncPublisher(KotlinModificationTopics.GLOBAL_MODULE_STATE_MODIFICATION)
    .onModification()
