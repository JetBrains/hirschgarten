package org.jetbrains.bazel.run.config

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
interface HotswappableRunConfiguration {
  fun getProject(): Project

  fun getAffectedTargets(): List<Label>
}
