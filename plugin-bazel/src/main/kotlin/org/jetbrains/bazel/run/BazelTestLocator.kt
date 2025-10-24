package org.jetbrains.bazel.run

import com.intellij.execution.Location
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.Unmodifiable

class BazelTestLocator : SMTestLocator {
  override fun getLocation(
    protocol: @NonNls String,
    path: @NonNls String,
    project: @NonNls Project,
    scope: GlobalSearchScope,
  ): @Unmodifiable List<Location<*>?> {
    TODO("Not yet implemented")
  }
}
