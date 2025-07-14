package org.jetbrains.bazel.run.config

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.CanonicalLabel

interface HotswappableRunConfiguration {
  fun getProject(): Project

  fun getAffectedTargets(): List<CanonicalLabel>
}
